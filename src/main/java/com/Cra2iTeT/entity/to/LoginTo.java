package com.Cra2iTeT.entity.to;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Cra2iTeT
 * @since 2023/1/26 16:18
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginTo {
    private String phone;
    private String pwdMd5;
}
