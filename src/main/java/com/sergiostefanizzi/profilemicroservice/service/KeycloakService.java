package com.sergiostefanizzi.profilemicroservice.service;

import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakService {
    @Autowired
    private Keycloak keycloak;
    private final String REALM_NAME = "social-accounts";

    public Boolean updateProfileList(String accountId, Long profileId) {
        UserResource userResource;
        try{
            userResource = this.keycloak
                    .realm(REALM_NAME)
                    .users()
                    .get(accountId);
        }catch (NotFoundException ex){
            log.info(ex.getMessage());
            return false;
        }

        UserRepresentation user = userResource.toRepresentation();

        Map<String, List<String>> attributes = user.getAttributes();

        List<String> profileList = attributes.get("profileList");
        if(profileList == null){
            profileList = Collections.singletonList(profileId.toString());
        } else if (!profileList.contains(profileId.toString())) {
            profileList.add(profileId.toString());
        }




        log.info("Updated JWT Profile list --> "+profileList);

        attributes.put("profileList", profileList);
        user.setAttributes(attributes);

        userResource.update(user);
        return true;
    }

    public Boolean removeFromProfileList(String accountId, Long profileId) {
        UserResource userResource;
        try{
            userResource = this.keycloak
                    .realm(REALM_NAME)
                    .users()
                    .get(accountId);
        }catch (NotFoundException ex){
            log.info(ex.getMessage());
            return false;
        }

        UserRepresentation user = userResource.toRepresentation();

        Map<String, List<String>> attributes = user.getAttributes();

        List<String> profileList = attributes.get("profileList");
        if(profileList != null){
            profileList.remove(profileId.toString());
        }



        log.info("Deleted JWT Profile list --> "+profileList);

        attributes.put("profileList", profileList);
        user.setAttributes(attributes);

        userResource.update(user);
        return true;
    }

    public Boolean isInProfileList(String accountId, Long profileId) {
        UserResource userResource;
        try{
            userResource = this.keycloak
                    .realm(REALM_NAME)
                    .users()
                    .get(accountId);
        }catch (NotFoundException ex){
            log.info(ex.getMessage());
            return false;
        }

        UserRepresentation user = userResource.toRepresentation();

        Map<String, List<String>> attributes = user.getAttributes();

        List<String> profileList = attributes.get("profileList");
        if(profileList != null){
            return profileList.contains(profileId.toString());
        }

        return false;
    }
}
