package com.Cra2iTeT.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Cra2iTeT
 * @since 2023/1/26 18:01
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserVo {
    private String token;
    private Long userId;
    private String phone;
}
