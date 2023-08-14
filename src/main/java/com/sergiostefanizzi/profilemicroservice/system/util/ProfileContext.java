package com.sergiostefanizzi.profilemicroservice.system.util;

import com.sergiostefanizzi.profilemicroservice.model.ProfileJpa;
import lombok.Getter;
import lombok.Setter;

public class ProfileContext {

    @Getter @Setter
    private ProfileJpa  profileJpa;

    //posso mettere altri jpa

    //questo e' un thread local
}
