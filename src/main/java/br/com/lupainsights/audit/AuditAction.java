package br.com.lupainsights.audit;

public enum AuditAction {
    AUTH_REGISTER,
    AUTH_LOGIN_SUCCESS,
    AUTH_LOGIN_FAILURE,
    CNPJ_IMPORT,
    CNPJ_CANCEL,
    CNPJ_DOWNLOAD,
    CNPJ_PREVIEW,
    PRODUCT_EVENT,
    ACCESS_DENIED,
    ADMIN_ACCESS,
    API_REQUEST
}
