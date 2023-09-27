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
        Alert alert = new Alert(source.getCreatedBy().getId(),
                source.getReason());
        alert.setId(source.getId());
        if (source.getComment() != null) alert.setCommentId(source.getComment().getId());
        if (source.getPost() != null) alert.setPostId(source.getPost().getId());
        if (source.getManagedByAccount() != null) alert.setManagedBy(source.getManagedByAccount());
        return alert;
    }
}
