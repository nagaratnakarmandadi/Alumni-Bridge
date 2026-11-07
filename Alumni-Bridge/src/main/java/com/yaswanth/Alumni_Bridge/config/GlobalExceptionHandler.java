package com.yaswanth.Alumni_Bridge.config;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({MaxUploadSizeExceededException.class, MultipartException.class})
    public String handleMaxSizeException(Exception exc,
                                         HttpServletRequest request,
                                         RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", "The uploaded file is too large. Please select a file under 10MB.");
        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isBlank() && !referer.contains("/save-profile")) {
            return "redirect:" + referer;
        }
        return "redirect:/edit-profile";
    }
}
