package com.sergiostefanizzi.profilemicroservice.model.converter;

import com.sergiostefanizzi.profilemicroservice.model.Follows;
import com.sergiostefanizzi.profilemicroservice.model.FollowsId;
import com.sergiostefanizzi.profilemicroservice.model.FollowsJpa;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class FollowsToFollowsJpaConverter implements Converter<Follows, FollowsJpa> {
    @Override
    public FollowsJpa convert(Follows source) {
        return new FollowsJpa(new FollowsId(source.getFollowerId(), source.getFollowedId()));
    }

    public Follows convertBack(FollowsJpa source){
        return new Follows(source.getFollower().getId(),
                source.getFollowed().getId(),
                source.getRequestStatus());
    }
}
