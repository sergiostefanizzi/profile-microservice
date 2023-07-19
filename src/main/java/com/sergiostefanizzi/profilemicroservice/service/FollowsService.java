package com.sergiostefanizzi.profilemicroservice.service;

import com.sergiostefanizzi.profilemicroservice.model.Follows;
import com.sergiostefanizzi.profilemicroservice.model.FollowsId;
import com.sergiostefanizzi.profilemicroservice.model.FollowsJpa;
import com.sergiostefanizzi.profilemicroservice.model.ProfileJpa;
import com.sergiostefanizzi.profilemicroservice.model.converter.FollowsToFollowsJpaConverter;
import com.sergiostefanizzi.profilemicroservice.repository.FollowsRepository;
import com.sergiostefanizzi.profilemicroservice.repository.ProfilesRepository;
import com.sergiostefanizzi.profilemicroservice.system.exception.UnfollowOnCreationException;
import com.sergiostefanizzi.profilemicroservice.system.exception.FollowNotFoundException;
import com.sergiostefanizzi.profilemicroservice.system.exception.ProfileNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FollowsService {
    private final FollowsRepository followsRepository;
    private final ProfilesRepository profilesRepository;
    private final FollowsToFollowsJpaConverter followsToFollowsJpaConverter;

    @Transactional
    public Follows addFollows(Long profileId, Long followsId, Boolean unfollow) {
        if(profileId == null || followsId == null){
            throw new ProfileNotFoundException("null");
        }
        FollowsJpa followsJpa;
        // Controllo l'esistenza dei due profili
        ProfileJpa profileJpa = this.profilesRepository.findNotDeletedById(profileId)
                .orElseThrow(() -> new ProfileNotFoundException(profileId));
        ProfileJpa followingJpa = this.profilesRepository.findNotDeletedById(followsId)
                .orElseThrow(() -> new ProfileNotFoundException(followsId));
        // Controllo che la risorsa non sia già stata creata
        Optional<FollowsJpa> optionalFollowsJpa = this.followsRepository.findById(new FollowsId(profileId, followsId));
        if (optionalFollowsJpa.isPresent()) {
            followsJpa = optionalFollowsJpa.get();
            if (followsJpa.getRequestStatus().equals(Follows.RequestStatusEnum.REJECTED) && !unfollow){
                // Se la richiesta e' stata rifiutata imposta in approvazione
                followsJpa.setRequestStatus(Follows.RequestStatusEnum.PENDING);
            } else if (!followsJpa.getRequestStatus().equals(Follows.RequestStatusEnum.REJECTED) && unfollow) {
                // Se la richiesta esiste ed e' gia' stata accettata o e' in approvazione
                // la imposto a rejected che vuol dire sia che l'utente a cui e' stata
                // mandata la richiesta l'ha rifiutata e sia che chi ha mandato la richiesta
                // e segue o e' ancora in attesa di seguire, non vuole piu' farlo
                followsJpa.setRequestStatus(Follows.RequestStatusEnum.REJECTED);
            }
        } else {
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
                followsJpa.setFollower(profileJpa);
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
        ProfileJpa profileJpa = this.profilesRepository.findNotDeletedById(profileId)
                .orElseThrow(() -> new ProfileNotFoundException(profileId));
        ProfileJpa followerJpa = this.profilesRepository.findNotDeletedById(followerId)
                .orElseThrow(() -> new ProfileNotFoundException(followerId));

        FollowsJpa followsJpa = this.followsRepository.findById(new FollowsId(followerId, profileId))
                .orElseThrow(FollowNotFoundException::new);
        // il profilo che fa reject ad un follower sostanzialmente elimina la richiesta di segui.
        // il profilo non puo' quindi accettare o rifiutare una richiesta inesistente
        // il follower puo' quindi ricreare una nuova richiesta che passa da rejected a pending in addFollows()
        // inoltre accetto anche le richieste di rimozione del follower
        if (followsJpa.getRequestStatus().equals(Follows.RequestStatusEnum.PENDING) || (followsJpa.getRequestStatus().equals(Follows.RequestStatusEnum.ACCEPTED) && rejectFollow)){

            followsJpa.setRequestStatus(rejectFollow ? Follows.RequestStatusEnum.REJECTED : Follows.RequestStatusEnum.ACCEPTED);

        }

        return this.followsToFollowsJpaConverter.convertBack(
                this.followsRepository.save(followsJpa));
    }
}
