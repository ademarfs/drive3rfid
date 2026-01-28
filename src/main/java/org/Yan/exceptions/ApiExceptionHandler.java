package org.Yan.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import org.Yan.exceptions.apiExceptions.TagListNotFoundException;
import org.Yan.exceptions.apiExceptions.TagNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler
    public ProblemDetail handleTagNotFound(TagNotFoundException ex, HttpServletRequest req){
        var pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        pd.setTitle("Tag não encontrada");
        pd.setDetail(ex.getMessage());
        pd.setProperty("path", req.getRequestURI());
        return pd;
    }

    @ExceptionHandler
    public ProblemDetail handleTagListNotFound(TagListNotFoundException ex, HttpServletRequest req){
        var pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        pd.setTitle("Lista de tags não encontrada");
        pd.setDetail(ex.getMessage());
        pd.setProperty("path", req.getRequestURI());
        return pd;
    }

    @ExceptionHandler({ConnectException.class, SocketTimeoutException.class})
    public ProblemDetail handleConnectionError(Exception ex, HttpServletRequest req){
        var pd = ProblemDetail.forStatus(HttpStatus.SERVICE_UNAVAILABLE);
        pd.setTitle("Erro de conexão com o sensor");
        pd.setDetail("Não foi possível conectar ao sensor RFID. Verifique se o sensor está ligado e acessível na rede. " + 
                     "Erro: " + ex.getMessage());
        pd.setProperty("path", req.getRequestURI());
        pd.setProperty("errorType", ex.getClass().getSimpleName());
        return pd;
    }

    @ExceptionHandler(UnknownHostException.class)
    public ProblemDetail handleUnknownHost(UnknownHostException ex, HttpServletRequest req){
        var pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Endereço do sensor inválido");
        pd.setDetail("O IP do sensor não pôde ser resolvido: " + ex.getMessage());
        pd.setProperty("path", req.getRequestURI());
        return pd;
    }

    @ExceptionHandler(RuntimeException.class)
    public ProblemDetail handleRuntimeException(RuntimeException ex, HttpServletRequest req){
        // Verifica se é uma exceção de conexão encapsulada
        Throwable cause = ex.getCause();
        if (cause instanceof ConnectException || cause instanceof SocketTimeoutException) {
            return handleConnectionError((Exception) cause, req);
        }
        if (cause instanceof UnknownHostException) {
            return handleUnknownHost((UnknownHostException) cause, req);
        }

        var pd = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        pd.setTitle("Erro interno do servidor");
        pd.setDetail("Ocorreu um erro ao processar a requisição. " + 
                     (cause != null ? "Causa: " + cause.getMessage() : ex.getMessage()));
        pd.setProperty("path", req.getRequestURI());
        pd.setProperty("errorType", ex.getClass().getSimpleName());
        return pd;
    }
}
