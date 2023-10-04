package com.sergiostefanizzi.profilemicroservice.service;

import com.sergiostefanizzi.profilemicroservice.model.PostJpa;
import com.sergiostefanizzi.profilemicroservice.model.converter.PostToPostJpaConverter;
import com.sergiostefanizzi.profilemicroservice.repository.PostsRepository;
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
}
