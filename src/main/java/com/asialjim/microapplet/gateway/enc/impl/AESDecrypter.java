/*
 * Copyright 2014-2025 <a href="mailto:asialjim@qq.com">Asial Jim</a>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.asialjim.microapplet.gateway.enc.impl;

import com.asialjim.microapplet.session.MamsUserEncKeyResParam;
import com.asialjim.microapplet.gateway.enc.Decrypter;
import com.asialjim.microapplet.gateway.enc.decrypt.EncryptionPacket;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Objects;

import static com.asialjim.microapplet.gateway.context.AuthenticateResCode.SignatureFailure;


/**
 * 基于AES对称加密的解密器
 *
 * @author Asial Jim
 * @version 1.0
 * @since 2026/2/9, &nbsp;&nbsp; <em>version:1.0</em>
 */
@Component
public class AESDecrypter implements Decrypter {
    private static final Logger logger = LoggerFactory.getLogger(AESDecrypter.class);

    /**
     * 加密类型
     */
    @Override
    public String supportEncType() {
        return AES;
    }

    @Override
    public byte[] decrypt(EncryptionPacket<?> encryptionPacket, MamsUserEncKeyResParam encryptKey) {
        // 密文
        String encrypt = encryptionPacket.encrypt();
        // 签名
        String signature = encryptionPacket.signature();

        // 用户秘钥
        String userKey = encryptKey.getUserKey();
        // 用户向量偏移量
        String iv = encryptKey.getIv();
        // 明文
        String plaintext = decrypt(encrypt, userKey, iv);

        // 生成签名
        String expectSignature = sign(encrypt, userKey);
        // 签名验证
        if (StringUtils.isBlank(expectSignature)) SignatureFailure.thr();
        if (!expectSignature.equals(signature))
            // 验证失败
            SignatureFailure.thr();

        if (StringUtils.isBlank(plaintext)) return new byte[0];

        return plaintext.getBytes(StandardCharsets.UTF_8);
    }

    public String sign(String params, String secret) {
        byte[] sha1Digest;
        StringBuilder sb = new StringBuilder();
        sb.append(secret);
        sb.append(params);
        sb.append(secret);
        try {
            sha1Digest = getSHA1Digest(sb.toString());
            if (Objects.nonNull(sha1Digest)) return byte2hex(sha1Digest);
            return StringUtils.EMPTY;
        } catch (IOException e) {
            if (logger.isDebugEnabled()) logger.debug("签名异常:{}", e.getMessage(), e);
            else logger.debug("签名异常:{}", e.getMessage());
            throw SignatureFailure.ex("签名异常");
        }
    }

    /**
     * 二进制转十六进制字符串
     *
     */
    private String byte2hex(byte[] bytes) {
        StringBuilder sign = new StringBuilder();
        for (byte aByte : bytes) {
            String hex = Integer.toHexString(aByte & 0xFF);
            if (hex.length() == 1) {
                sign.append("0");
            }
            sign.append(hex.toUpperCase());
        }
        return sign.toString();
    }


    private byte[] getSHA1Digest(String data) throws IOException {
        byte[] bytes;
        try {
            MessageDigest md = MessageDigest.getInstance(SHA_1);
            bytes = md.digest(data.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException gse) {
            throw new IOException(gse.getMessage());
        }
        return bytes;
    }

    public String decrypt(String source, String key, String ivStr) {
        if (key == null) {
            logger.error("Key为空null");
            return null;
        } else if (StringUtils.isBlank(source)) {
            logger.error("空密文");
            return StringUtils.EMPTY;
        } else {
            try {
                byte[] ex = key.getBytes(StandardCharsets.US_ASCII);
                SecretKeySpec skeySpec = new SecretKeySpec(ex, AES);
                Cipher cipher = Cipher.getInstance(AES_CBC_PKCS5Padding);
                IvParameterSpec iv = new IvParameterSpec(ivStr.getBytes());
                cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
                byte[] encrypted1 = parseHexStr2Byte(source);
                if (Objects.isNull(encrypted1) || ArrayUtils.isEmpty(encrypted1)) {
                    return StringUtils.EMPTY;
                }
                byte[] e = cipher.doFinal(encrypted1);
                return new String(e, StandardCharsets.UTF_8);
            } catch (Exception var9) {
                logger.error("Decryption error occurred", var9);
                return null;
            }
        }
    }

    private byte[] parseHexStr2Byte(String hexStr) {
        if (StringUtils.isBlank(hexStr)) return null;

        byte[] result = new byte[hexStr.length() / 2];

        for (int i = 0; i < hexStr.length() / 2; ++i) {
            int high = Integer.parseInt(hexStr.substring(i * 2, i * 2 + 1), 16);
            int low = Integer.parseInt(hexStr.substring(i * 2 + 1, i * 2 + 2), 16);
            result[i] = (byte) (high * 16 + low);
        }

        return result;
    }
}