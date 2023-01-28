package com.Cra2iTeT.entity.pojo;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Cra2iTeT
 * @since 2023/1/26 16:04
 */
@TableName("user")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class User {
    private Long userId;
    private String phone;
    private String pwdMd5;
}
