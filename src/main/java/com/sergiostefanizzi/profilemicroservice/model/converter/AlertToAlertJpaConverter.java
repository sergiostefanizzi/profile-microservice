package com.sergiostefanizzi.profilemicroservice.model.converter;

import com.sergiostefanizzi.profilemicroservice.model.Alert;
import com.sergiostefanizzi.profilemicroservice.model.AlertJpa;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class AlertToAlertJpaConverter implements Converter<Alert, AlertJpa> {
    @Override
    public AlertJpa convert(Alert source) {
        return new AlertJpa(source.getReason());
    }

    public Alert convertBack(AlertJpa source){
        Alert alert = new Alert(source.getId(),
                source.getCreatedBy().getId(),
                source.getReason());
        if (source.getComment().getId() != null) alert.setCommentId(source.getComment().getId());
        if (source.getPost().getId() != null) alert.setCommentId(source.getPost().getId());
        if (source.getManagedBy().getId() != null) alert.setManagedBy(source.getManagedBy().getId());
        return alert;
    }
}