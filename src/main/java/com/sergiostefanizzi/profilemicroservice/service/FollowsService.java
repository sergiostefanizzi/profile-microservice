package com.sergiostefanizzi.profilemicroservice.service;

import com.sergiostefanizzi.profilemicroservice.model.*;
import com.sergiostefanizzi.profilemicroservice.model.converter.FollowsToFollowsJpaConverter;
import com.sergiostefanizzi.profilemicroservice.model.converter.ProfileToProfileJpaConverter;
import com.sergiostefanizzi.profilemicroservice.repository.FollowsRepository;
import com.sergiostefanizzi.profilemicroservice.repository.ProfilesRepository;
import com.sergiostefanizzi.profilemicroservice.system.exception.UnfollowOnCreationException;
import com.sergiostefanizzi.profilemicroservice.system.exception.FollowNotFoundException;
import com.sergiostefanizzi.profilemicroservice.system.exception.ProfileNotFoundException;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FollowsService {
    private final FollowsRepository followsRepository;
    private final ProfilesRepository profilesRepository;
    private final FollowsToFollowsJpaConverter followsToFollowsJpaConverter;
    private final ProfileToProfileJpaConverter profileToProfileJpaConverter;
    @Autowired
    EntityManager entityManager;


    @Transactional
    public Follows addFollows(Long profileId, Long followsId, Boolean unfollow) {
        if(profileId == null || followsId == null){
            throw new ProfileNotFoundException("Missing input parameter");
        }

        FollowsJpa followsJpa;
        // Controllo che la risorsa non sia già stata creata
        Optional<FollowsJpa> optionalFollowsJpa = this.followsRepository.findById(new FollowsId(profileId, followsId));
        // TODO scompongo l'if in due metodi privati
        if (optionalFollowsJpa.isPresent()) {
            followsJpa = optionalFollowsJpa.get();
            if (followsJpa.getRequestStatus().equals(Follows.RequestStatusEnum.REJECTED) && !unfollow){
                // Se la richiesta e' stata rifiutata imposta in approvazione
                followsJpa.setRequestStatus(Follows.RequestStatusEnum.PENDING);
                followsJpa.setUnfollowedAt(null);//reset a null eventuale data di unfollowed
            } else if (!followsJpa.getRequestStatus().equals(Follows.RequestStatusEnum.REJECTED) && unfollow) {
                // Se la richiesta esiste ed e' gia' stata accettata o e' in approvazione
                // la imposto a rejected che vuol dire sia che l'utente a cui e' stata
                // mandata la richiesta l'ha rifiutata e sia che chi ha mandato la richiesta
                // e segue o e' ancora in attesa di seguire, non vuole piu' farlo
                followsJpa.setRequestStatus(Follows.RequestStatusEnum.REJECTED);
                followsJpa.setUnfollowedAt(LocalDateTime.now());
            }
        } else {
            // Controllo l'esistenza dei due profili
            // ProfileJpa profileJpa = this.profilesRepository.findActiveById(profileId)
            //        .orElseThrow(() -> new ProfileNotFoundException(profileId));
            ProfileJpa followingJpa = this.profilesRepository.findActiveById(followsId)
                    .orElseThrow(() -> new ProfileNotFoundException(followsId));
            // il profileId esiste perche' verificato nell'interceptor
            if(!unfollow){
                // Se la richiesta non esiste
                followsJpa = new FollowsJpa(new FollowsId(profileId, followsId));
                // Se il profilo e' privato la richiesta va in approvazione
                // altrimenti viene subito accettata
                if (followingJpa.getIsPrivate()){
                    followsJpa.setRequestStatus(Follows.RequestStatusEnum.PENDING);
                } else {
                    followsJpa.setRequestStatus(Follows.RequestStatusEnum.ACCEPTED);
                    followsJpa.setFollowedAt(LocalDateTime.now());
                }

                entityManager.flush();
                ProfileJpa profileJpaTemp = entityManager.getReference(ProfileJpa.class, profileId);
                followsJpa.setFollower(profileJpaTemp);
                followsJpa.setFollowed(followingJpa);
            }else {
                throw new UnfollowOnCreationException();
            }
        }
        return this.followsToFollowsJpaConverter.convertBack(
                this.followsRepository.save(followsJpa));
    }

    @Transactional
    public Follows acceptFollows(Long profileId, Long followerId, Boolean rejectFollow) {
        if(profileId == null || followerId == null){
            throw new ProfileNotFoundException("null");
        }

        // Controllo l'esistenza dei due profili
        this.profilesRepository.findActiveById(profileId)
                .orElseThrow(() -> new ProfileNotFoundException(profileId));
        this.profilesRepository.findActiveById(followerId)
                .orElseThrow(() -> new ProfileNotFoundException(followerId));

        FollowsJpa followsJpa = this.followsRepository.findById(new FollowsId(followerId, profileId))
                .orElseThrow(FollowNotFoundException::new);
        // il profilo che fa reject ad un follower sostanzialmente elimina la richiesta di segui.
        // il profilo non puo' quindi accettare o rifiutare una richiesta inesistente
        // il follower puo' quindi ricreare una nuova richiesta che passa da rejected a pending in addFollows()
        // inoltre accetto anche le richieste di rimozione del follower

        if (followsJpa.getRequestStatus().equals(Follows.RequestStatusEnum.PENDING)){
            if(rejectFollow){
                followsJpa.setRequestStatus(Follows.RequestStatusEnum.REJECTED);
            }else{
                followsJpa.setRequestStatus(Follows.RequestStatusEnum.ACCEPTED);
                followsJpa.setFollowedAt(LocalDateTime.now());
            }
        }

        if (followsJpa.getRequestStatus().equals(Follows.RequestStatusEnum.ACCEPTED)){
            if(rejectFollow){
                followsJpa.setRequestStatus(Follows.RequestStatusEnum.REJECTED);
                followsJpa.setUnfollowedAt(LocalDateTime.now());
            }
        }


        return this.followsToFollowsJpaConverter.convertBack(
                this.followsRepository.save(followsJpa));
    }

    @Transactional
    public ProfileFollowList findAllFollowers(Long profileId) {
        if(profileId == null){
            throw new ProfileNotFoundException("null");
        }

        //controllo l'esistenza del profilo
        ProfileJpa profileJpa = this.profilesRepository.findActiveById(profileId)
                .orElseThrow(() -> new ProfileNotFoundException(profileId));
        // Ritorno la lista dei profili dei follower
        List<Profile> followerList = this.followsRepository.findActiveFollowers(profileJpa)
                .stream().map(this.profileToProfileJpaConverter::convertBack).toList();
        return new ProfileFollowList(followerList, followerList.size());
    }

    @Transactional
    public ProfileFollowList findAllFollowings(Long profileId) {
        if(profileId == null){
            throw new ProfileNotFoundException("null");
        }

        //controllo l'esistenza del profilo
        ProfileJpa profileJpa = this.profilesRepository.findActiveById(profileId)
                .orElseThrow(() -> new ProfileNotFoundException(profileId));
        // Ritorno la lista dei profili dei follower
        List<Profile> followerList = this.followsRepository.findActiveFollowings(profileJpa)
                .stream().map(this.profileToProfileJpaConverter::convertBack).toList();
        return new ProfileFollowList(followerList, followerList.size());
    }
}
