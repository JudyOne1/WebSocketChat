package com.itheima.pojo;

import lombok.Data;

/**
 * 用来封装http请求的响应数据
 */
@Data
public class Result {
    private boolean flag;
    private String message;
}
