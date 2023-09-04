package com.sergiostefanizzi.profilemicroservice.service;

import com.sergiostefanizzi.profilemicroservice.model.*;
import com.sergiostefanizzi.profilemicroservice.model.converter.FollowsToFollowsJpaConverter;
import com.sergiostefanizzi.profilemicroservice.model.converter.ProfileToProfileJpaConverter;
import com.sergiostefanizzi.profilemicroservice.repository.FollowsRepository;
import com.sergiostefanizzi.profilemicroservice.repository.ProfilesRepository;
import com.sergiostefanizzi.profilemicroservice.system.exception.FollowNotFoundException;
import com.sergiostefanizzi.profilemicroservice.system.exception.ProfileNotFoundException;
import com.sergiostefanizzi.profilemicroservice.system.exception.UnfollowOnCreationException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    /*
    @Autowired
    EntityManager entityManager;
     */


    @Transactional
    public Follows addFollows(Long profileId, Long profileToFollowId, Boolean isUnfollow) {
        if(profileId == null || profileToFollowId == null){
            throw new ProfileNotFoundException("Missing input parameter");
        }

        FollowsJpa followsRequestJpa;
        // Controllo che la risorsa non sia già stata creata
        // TODO this.followsRepository.findActiveById
        Optional<FollowsJpa> optionalFollowsRequestJpa = this.followsRepository.findById(new FollowsId(profileId, profileToFollowId));
        // TODO scompongo l'if in due metodi privati
        if (optionalFollowsRequestJpa.isPresent()) {
            followsRequestJpa = updateOldFollowRequest(isUnfollow, optionalFollowsRequestJpa.get());
        } else {
            followsRequestJpa = createNewFollowRequest(profileId, profileToFollowId, isUnfollow);
        }
        return this.followsToFollowsJpaConverter.convertBack(
                this.followsRepository.save(followsRequestJpa));
    }

    private static FollowsJpa updateOldFollowRequest(Boolean isUnfollow, FollowsJpa followsRequestJpa) {
        if (followsRequestJpa.getRequestStatus().equals(Follows.RequestStatusEnum.REJECTED) && !isUnfollow){
            // Se la richiesta esiste ed e' stata rifiutata e viene inviata una nuova richiesta, imposta in approvazione
            followsRequestJpa.setRequestStatus(Follows.RequestStatusEnum.PENDING);
            followsRequestJpa.setUnfollowedAt(null);//reset a null eventuale data di unfollowed
        } else if (!followsRequestJpa.getRequestStatus().equals(Follows.RequestStatusEnum.REJECTED) && isUnfollow) {
            // Se la richiesta esiste ed e' gia' stata accettata o e' in approvazione e viene
            // inviata una richiesta di unfollow,
            // la imposto a rejected che vuol dire sia che l'utente a cui e' stata
            // mandata la richiesta l'ha rifiutata e sia che chi ha mandato la richiesta
            // e segue o e' ancora in attesa di seguire, non vuole piu' farlo
            followsRequestJpa.setRequestStatus(Follows.RequestStatusEnum.REJECTED);
            followsRequestJpa.setUnfollowedAt(LocalDateTime.now());
        }
        return followsRequestJpa;
    }
    private FollowsJpa createNewFollowRequest(Long profileId, Long profileToFollowId, Boolean isUnfollow) {
        FollowsJpa followsRequestJpa;
        // Controllo l'esistenza dei due profili
        /*
        ProfileJpa profileJpa = this.profilesRepository.findActiveById(profileId)
                .orElseThrow(() -> new ProfileNotFoundException(profileId));
        ProfileJpa profileToFollowJpa = this.profilesRepository.findActiveById(profileToFollowId)
                .orElseThrow(() -> new ProfileNotFoundException(profileToFollowId));
         */

        ProfileJpa profileJpa = this.profilesRepository.getReferenceById(profileId);
        ProfileJpa profileToFollowJpa = this.profilesRepository.getReferenceById(profileToFollowId);
        // il profileId esiste perche' verificato nell'interceptor
        if(!isUnfollow){
            // Se la richiesta non esiste
            followsRequestJpa = new FollowsJpa(new FollowsId(profileId, profileToFollowId));
            // Se il profilo e' privato la richiesta va in approvazione
            // altrimenti viene subito accettata
            if (profileToFollowJpa.getIsPrivate()){
                followsRequestJpa.setRequestStatus(Follows.RequestStatusEnum.PENDING);
            } else {
                followsRequestJpa.setRequestStatus(Follows.RequestStatusEnum.ACCEPTED);
                followsRequestJpa.setFollowedAt(LocalDateTime.now());
            }

            //entityManager.flush();
            //ProfileJpa profileJpa = entityManager.getReference(ProfileJpa.class, profileId);
            followsRequestJpa.setFollower(profileJpa);
            followsRequestJpa.setFollowed(profileToFollowJpa);
        }else {
            throw new UnfollowOnCreationException();
        }
        return followsRequestJpa;
    }



    @Transactional
    public Follows acceptFollows(Long profileId, Long followerId, Boolean rejectFollow) {
        if(profileId == null || followerId == null){
            throw new ProfileNotFoundException("Missing input parameter");
        }
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

        if (followsJpa.getRequestStatus().equals(Follows.RequestStatusEnum.ACCEPTED) && rejectFollow){
                followsJpa.setRequestStatus(Follows.RequestStatusEnum.REJECTED);
                followsJpa.setUnfollowedAt(LocalDateTime.now());
        }


        return this.followsToFollowsJpaConverter.convertBack(
                this.followsRepository.save(followsJpa));
    }

    @Transactional
    public ProfileFollowList findAllFollowers(Long profileId) {
        if(profileId == null){
            throw new ProfileNotFoundException("Missing input parameter");
        }

        ProfileJpa profileJpa = this.profilesRepository.getReferenceById(profileId);
        // Ritorno la lista dei profili dei follower
        List<Profile> followerList = this.followsRepository.findActiveFollowers(profileJpa)
                .stream().map(this.profileToProfileJpaConverter::convertBack).toList();
        //TODO Ritorno il numero di followers ma non la lista dei profili se non ho accesso a tale profilo
        return new ProfileFollowList(followerList, followerList.size());
    }

    @Transactional
    public ProfileFollowList findAllFollowings(Long profileId) {
        if(profileId == null){
            throw new ProfileNotFoundException("Missing input parameter");
        }
        ProfileJpa profileJpa = this.profilesRepository.getReferenceById(profileId);
        // Ritorno la lista dei profili dei follower
        List<Profile> followerList = this.followsRepository.findActiveFollowings(profileJpa)
                .stream().map(this.profileToProfileJpaConverter::convertBack).toList();
        //TODO Ritorno il numero di followings ma non la lista dei profili se non ho accesso a tale profilo
        return new ProfileFollowList(followerList, followerList.size());
    }
}
