/*
 * Copyright 2019-2021 Mamoe Technologies and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/mamoe/mirai/blob/dev/LICENSE
 */

package net.mamoe.mirai.internal.utils.crypto

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.mamoe.mirai.utils.decodeBase64
import net.mamoe.mirai.utils.hexToBytes
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec

internal expect interface ECDHPrivateKey

internal expect interface ECDHPublicKey {
    fun getEncoded(): ByteArray
}

internal expect class ECDHKeyPairImpl : ECDHKeyPair

internal interface ECDHKeyPair {
    val privateKey: ECDHPrivateKey
    val publicKey: ECDHPublicKey

    /**
     * 私匙和动态公匙([ECDHInitialPublicKey]) 计算得到的 shareKey
     */
    val maskedShareKey: ByteArray

    /**
     * 私匙和动态公匙([ECDHInitialPublicKey]) 计算得到的 publicKey
     */
    val maskedPublicKey: ByteArray

    object DefaultStub : ECDHKeyPair {
        val defaultPublicKey =
            "04edb8906046f5bfbe9abbc5a88b37d70a6006bfbabc1f0cd49dfb33505e63efc5d78ee4e0a4595033b93d02096dcd3190279211f7b4f6785079e19004aa0e03bc".hexToBytes()
        val defaultShareKey = "c129edba736f4909ecc4ab8e010f46a3".hexToBytes()

        override val privateKey: Nothing get() = error("stub!")
        override val publicKey: Nothing get() = error("stub!")
        override val maskedShareKey: ByteArray get() = defaultShareKey
        override val maskedPublicKey: ByteArray
            get() = defaultPublicKey
    }
}

/**
 * 椭圆曲线密码, ECDH 加密
 */
internal expect class ECDH(keyPair: ECDHKeyPair) {
    val keyPair: ECDHKeyPair

    /**
     * 由 [keyPair] 的私匙和 [peerPublicKey] 计算 shareKey
     */
    fun calculateShareKeyByPeerPublicKey(peerPublicKey: ECDHPublicKey): ByteArray

    companion object {
        val isECDHAvailable: Boolean

        /**
         * 由完整的 publicKey ByteArray 得到 [ECDHPublicKey]
         */
        fun constructPublicKey(key: ByteArray): ECDHPublicKey

        /**
         * 由完整的 rsaKey 校验 publicKey
         */
        fun verifyPublicKey(version: Int, publicKey: String, publicKeySign: String): Boolean

        /**
         * 生成随机密匙对
         */
        fun generateKeyPair(initialPublicKey: ECDHPublicKey = defaultInitialPublicKey.key): ECDHKeyPair

        /**
         * 由一对密匙计算 shareKey
         */
        fun calculateShareKey(privateKey: ECDHPrivateKey, publicKey: ECDHPublicKey): ByteArray
    }

    override fun toString(): String
}

@Suppress("FunctionName")
internal fun ecdhWithPublicKey(initialPublicKey: ECDHInitialPublicKey = defaultInitialPublicKey): ECDHWithPublicKey =
    ECDHWithPublicKey(initialPublicKey)

internal data class ECDHWithPublicKey(private val initialPublicKey: ECDHInitialPublicKey = defaultInitialPublicKey) {
    private val ecdhInstance: ECDH = ECDH(ECDH.generateKeyPair(initialPublicKey.key))
    val version: Int = initialPublicKey.version
    val keyPair: ECDHKeyPair = ecdhInstance.keyPair

    /**
     * 由 [keyPair] 的私匙和 [peerPublicKey] 计算 shareKey
     */
    fun calculateShareKeyByPeerPublicKey(peerPublicKey: ECDHPublicKey): ByteArray =
        ecdhInstance.calculateShareKeyByPeerPublicKey(peerPublicKey)

}
// gen by p-256
//3059301306072A8648CE3D020106082A8648CE3D03010703420004FA540CB3F755D0A6572338777A4D0BEAFA86664D53040B27331CBF1B7F3C226CE8A1C05EFA9028F85510B103D8175172895C9F9FE4C80A47894BCA2BE569BFCB
//3059301306072A8648CE3D020106082A8648CE3D03010703420004949D41D7C14B92F0CB94B6232FB87BA51B0D5AB661FBAF95599A97472FFC4F50BC8CEC5865E79DB3782459A6E9A2298954CD198A25274CEEA8F925342D763D62

/*
// p-256
    get() = ECDH.constructPublicKey(
        ("3059301306072A8648CE3D020106082A8648CE3D03010703420004" +
                "EBCA94D733E399B2DB96EACDD3F69A8BB0F74224E2B44E3357812211D2E62EFB" +
                "C91BB553098E25E33A799ADC7F76FEB208DA7C6522CDB0719A305180CC54A82E"
                ).chunkedHexToBytes()
    )
* */

@Serializable
internal data class ECDHInitialPublicKey(val version: Int = 1, val keyStr: String, val expireTime: Long = 0) {
    @Transient
    internal val key: ECDHPublicKey = keyStr.adjustToPublicKey()
}

internal val publicKeyForVerify by lazy {
    KeyFactory.getInstance("RSA")
        .generatePublic(X509EncodedKeySpec("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAuJTW4abQJXeVdAODw1CamZH4QJZChyT08ribet1Gp0wpSabIgyKFZAOxeArcCbknKyBrRY3FFI9HgY1AyItH8DOUe6ajDEb6c+vrgjgeCiOiCVyum4lI5Fmp38iHKH14xap6xGaXcBccdOZNzGT82sPDM2Oc6QYSZpfs8EO7TYT7KSB2gaHz99RQ4A/Lel1Vw0krk+DescN6TgRCaXjSGn268jD7lOO23x5JS1mavsUJtOZpXkK9GqCGSTCTbCwZhI33CpwdQ2EHLhiP5RaXZCio6lksu+d8sKTWU1eEiEb3cQ7nuZXLYH7leeYFoPtbFV4RicIWp0/YG+RP7rLPCwIDAQAB".decodeBase64()))
}
internal val defaultInitialPublicKey: ECDHInitialPublicKey by lazy { ECDHInitialPublicKey(keyStr = "04EBCA94D733E399B2DB96EACDD3F69A8BB0F74224E2B44E3357812211D2E62EFBC91BB553098E25E33A799ADC7F76FEB208DA7C6522CDB0719A305180CC54A82E") }
private val signHead = "3059301306072a8648ce3d020106082a8648ce3d030107034200".hexToBytes()

internal fun String.adjustToPublicKey(): ECDHPublicKey {
    return this.hexToBytes().adjustToPublicKey()
}

internal fun ByteArray.adjustToPublicKey(): ECDHPublicKey {

    return ECDH.constructPublicKey(signHead + this)
}