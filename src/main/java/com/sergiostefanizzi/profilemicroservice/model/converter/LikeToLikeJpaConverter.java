package com.sergiostefanizzi.profilemicroservice.model.converter;

import com.sergiostefanizzi.profilemicroservice.model.Like;
import com.sergiostefanizzi.profilemicroservice.model.LikeId;
import com.sergiostefanizzi.profilemicroservice.model.LikeJpa;
import org.springframework.core.convert.converter.Converter;

public class LikeToLikeJpaConverter implements Converter<Like, LikeJpa> {
    @Override
    public LikeJpa convert(Like source) {
        return new LikeJpa(new LikeId(source.getProfileId(), source.getPostId()));
    }

    public Like convertBack(LikeJpa source){
        return new Like(source.getLikesId().getProfileId(), source.getLikesId().getPostId());
    }
}
