package com.thelook.enums;

public enum ImageProcessStatus {
    PENDING,        // Recebido pelo banco, mas ainda não enviado/pego pela fila
    PROCESSING,     // O Worker já pegou a imagem e está compactando
    READY,          // WebP gerado e disponível no Storage
    FAILED          // Houve um erro (formato inválido, queda do storage, etc)
}
