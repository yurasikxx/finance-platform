package by.bsuir.fp.controller.dto;

import lombok.Data;

@Data
public class AuthRequest {
    private String email;
    private String password;
}