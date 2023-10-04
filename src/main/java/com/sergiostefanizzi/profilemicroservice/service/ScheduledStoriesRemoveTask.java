package com.sergiostefanizzi.profilemicroservice.service;

import com.sergiostefanizzi.profilemicroservice.model.PostJpa;
import com.sergiostefanizzi.profilemicroservice.model.ProfileJpa;
import com.sergiostefanizzi.profilemicroservice.repository.PostsRepository;
import com.sergiostefanizzi.profilemicroservice.repository.ProfilesRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledStoriesRemoveTask {
    private final PostsRepository postsRepository;
    private final ProfilesRepository profilesRepository;
    // aggiungere random
    @Scheduled(fixedRate = 5000L)
    @Transactional
    public void removeOutdatedStories(){
        LocalDateTime removalTime = LocalDateTime.now();
        List<PostJpa> storiesToRemove = this.postsRepository.getOutdatedStories(removalTime.minusDays(1));
        if (!storiesToRemove.isEmpty()){
            storiesToRemove.forEach(story -> story.setDeletedAt(removalTime));
            int removedStories = this.postsRepository.saveAll(storiesToRemove).size();
            log.info("Scheduler: Storie rimosse :"+removedStories);
        }else {
            log.info("Scheduler: Nessuna storia rimossa");
        }
    }

    @Scheduled(fixedRate = 3000L)
    @Transactional
    public void unblockProfiles(){
        List<ProfileJpa> profileToUnblockList = this.profilesRepository.findAllToBeUnBlocked(LocalDateTime.now());
        if (!profileToUnblockList.isEmpty()){
            profileToUnblockList.forEach(profile -> profile.setBlockedUntil(null));
            int removedStories = this.profilesRepository.saveAll(profileToUnblockList).size();
            log.info("Scheduler: Profili sbloccati :"+removedStories);
        }else {
            log.info("Scheduler: Nessun profilo sbloccato");
        }
    }
}
