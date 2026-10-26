package com.onedao.auth.dto;

/** Deliberately not bean-validated: any bad/missing credential must yield "Login Failed". */
public record LoginRequest(String username, String password) {}
