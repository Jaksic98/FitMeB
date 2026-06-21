package com.consi.fitme.exception;

import com.consi.fitme.dto.response.ErrorResponseDTO;
import com.consi.fitme.exception.base.CustomError;
import com.consi.fitme.model.ErrorCode;
import com.consi.fitme.util.ErrorResponseUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {
  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
  private static final String LOG_PATTERN = "[{}] {} - {}: {}";

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponseDTO> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex, HttpServletRequest request) {
    List<String> details = new ArrayList<>();
    ex.getBindingResult()
        .getFieldErrors()
        .forEach(
            fieldError ->
                details.add(fieldError.getField() + ": " + fieldError.getDefaultMessage()));
    ex.getBindingResult()
        .getGlobalErrors()
        .forEach(
            objectError ->
                details.add(objectError.getObjectName() + ": " + objectError.getDefaultMessage()));

    logWarn(ex, request);
    return buildErrorResponse(
        ErrorCode.VALIDATION_FAILED, ErrorCode.VALIDATION_FAILED.getMessage(), details);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponseDTO> handleConstraintViolation(
      ConstraintViolationException ex, HttpServletRequest request) {
    List<String> details =
        ex.getConstraintViolations().stream()
            .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
            .toList();

    logWarn(ex, request);
    return buildErrorResponse(
        ErrorCode.VALIDATION_FAILED, ErrorCode.VALIDATION_FAILED.getMessage(), details);
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ErrorResponseDTO> handleMissingServletRequestParameter(
      MissingServletRequestParameterException ex, HttpServletRequest request) {
    List<String> details = List.of("Parametar je obavezan: " + ex.getParameterName());

    logWarn(ex, request);
    return buildErrorResponse(
        ErrorCode.VALIDATION_FAILED, ErrorCode.VALIDATION_FAILED.getMessage(), details);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponseDTO> handleMethodArgumentTypeMismatch(
      MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
    String parameterName = ex.getName();
    Class<?> requiredType = ex.getRequiredType();
    String expectedType = requiredType != null ? requiredType.getSimpleName() : "ispravan tip";
    List<String> details =
        List.of("Parametar " + parameterName + " mora biti tipa " + expectedType);

    logWarn(ex, request);
    return buildErrorResponse(
        ErrorCode.VALIDATION_FAILED, ErrorCode.VALIDATION_FAILED.getMessage(), details);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponseDTO> handleHttpMessageNotReadable(
      HttpMessageNotReadableException ex, HttpServletRequest request) {
    List<String> details = List.of("Zahtev je neispravan ili nedostaju obavezna polja");

    logWarn(ex, request);
    return buildErrorResponse(
        ErrorCode.VALIDATION_FAILED, ErrorCode.VALIDATION_FAILED.getMessage(), details);
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ErrorResponseDTO> handleMethodNotSupported(
      HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
    List<String> details =
        List.of("HTTP metoda " + ex.getMethod() + " nije podržana za ovu putanju");

    logWarn(ex, request);
    return buildErrorResponse(
        ErrorCode.VALIDATION_FAILED, ErrorCode.VALIDATION_FAILED.getMessage(), details);
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<ErrorResponseDTO> handleMediaTypeNotSupported(
      HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
    List<String> details = List.of("Content-Type nije podržan: " + ex.getContentType());

    logWarn(ex, request);
    return buildErrorResponse(
        ErrorCode.VALIDATION_FAILED, ErrorCode.VALIDATION_FAILED.getMessage(), details);
  }

  @ExceptionHandler(NoHandlerFoundException.class)
  public ResponseEntity<ErrorResponseDTO> handleNoHandlerFound(
      NoHandlerFoundException ex, HttpServletRequest request) {
    List<String> details = List.of("Putanja nije pronađena: " + ex.getRequestURL());

    logWarn(ex, request);
    return buildErrorResponse(
        ErrorCode.RESOURCE_NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND.getMessage(), details);
  }

  @ExceptionHandler(AuthorizationDeniedException.class)
  public ResponseEntity<ErrorResponseDTO> handleAuthorizationDenied(
      AuthorizationDeniedException ex, HttpServletRequest request) {
    List<String> details =
        List.of(
            "Nemate pravo pristupa ovom resursu",
            "Putanja: " + request.getMethod() + " " + request.getRequestURI());

    logWarn(ex, request);
    return buildErrorResponse(
        ErrorCode.ACCESS_DENIED, ErrorCode.ACCESS_DENIED.getMessage(), details);
  }

  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<ErrorResponseDTO> handleRuntimeException(
      RuntimeException ex, HttpServletRequest request) {
    if (!(ex instanceof CustomError customError)) {
      return handleGeneralException(ex, request);
    }

    ErrorCode errorCode = customError.getErrorCode();
    List<String> details = List.of(ex.getMessage());

    logError(ex, request);
    return buildErrorResponse(errorCode, errorCode.getMessage(), details);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponseDTO> handleGeneralException(
      Exception ex, HttpServletRequest request) {
    logError(ex, request);
    return buildErrorResponse(
        ErrorCode.INTERNAL_SERVER_ERROR,
        ErrorCode.INTERNAL_SERVER_ERROR.getMessage(),
        List.of("Došlo je do neočekivane greške"));
  }

  private ResponseEntity<ErrorResponseDTO> buildErrorResponse(
      ErrorCode errorCode, String message, List<String> details) {
    return ErrorResponseUtil.response(errorCode, message, details);
  }

  private void logWarn(Exception ex, HttpServletRequest request) {
    logger.warn(
        LOG_PATTERN,
        request.getMethod(),
        request.getRequestURI(),
        ex.getClass().getSimpleName(),
        ex.getMessage());
  }

  private void logError(Exception ex, HttpServletRequest request) {
    logger.error(
        LOG_PATTERN,
        request.getMethod(),
        request.getRequestURI(),
        ex.getClass().getSimpleName(),
        ex.getMessage(),
        ex);
  }
}
