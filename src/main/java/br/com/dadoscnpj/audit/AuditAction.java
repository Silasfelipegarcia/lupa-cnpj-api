package br.com.dadoscnpj.audit;

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
    API_REQUEST
}
