package com.sergiostefanizzi.profilemicroservice.model.converter;

import com.sergiostefanizzi.profilemicroservice.model.Profile;
import com.sergiostefanizzi.profilemicroservice.model.ProfileJpa;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.net.URI;


@Component
public class ProfileToProfileJpaConverter implements Converter<Profile, ProfileJpa> {
    @Override
    public ProfileJpa convert(Profile source) {
        ProfileJpa profileJpa = new ProfileJpa(
                source.getProfileName(),
                source.getIsPrivate(),
                source.getAccountId()
        );
        profileJpa.setBio(source.getBio());
        profileJpa.setPictureUrl(source.getPictureUrl());
        return profileJpa;
    }

    public Profile convertBack(ProfileJpa source){
        Profile profile = new Profile(
                source.getProfileName(),
                source.getIsPrivate(),
                source.getAccountId()
        );
        profile.setBio(source.getBio());
        profile.setPictureUrl(source.getPictureUrl());
        profile.setId(source.getId());
        return profile;
    }
}
