package com.sergiostefanizzi.profilemicroservice.service;

import com.sergiostefanizzi.profilemicroservice.model.Profile;
import com.sergiostefanizzi.profilemicroservice.model.ProfileAdminPatch;
import com.sergiostefanizzi.profilemicroservice.model.ProfileJpa;
import com.sergiostefanizzi.profilemicroservice.model.converter.ProfileToProfileJpaConverter;
import com.sergiostefanizzi.profilemicroservice.repository.ProfilesRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.time.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@Slf4j
public class AdminsServiceTest {
    @InjectMocks
    private AdminsService adminsService;
    @Mock
    private ProfilesRepository profilesRepository;
    @Mock
    ProfileToProfileJpaConverter profileToProfileJpaConverter;
    private Long profileId = 1L;
    private Long accountId = 1L;
    private String profileName = "pinco_pallino";


    @BeforeEach
    void setUp() {

    }

    @AfterEach
    void tearDown() {

    }

    @Test
    void testBlockProfileById_Block_Success(){

        ProfileAdminPatch profileAdminPatch = new ProfileAdminPatch();
        profileAdminPatch.setBlockedUntil(OffsetDateTime.of(
                LocalDate.of(2023,10,5),
                LocalTime.of(12,0),
                ZoneOffset.UTC
        ));

        ProfileJpa profileToUpdate = new ProfileJpa(profileName, false, accountId);
        profileToUpdate.setCreatedAt(LocalDateTime.of(
                LocalDate.of(2023,1,1),
                LocalTime.of(12,0)
        ));
        profileToUpdate.setId(profileId);

        Profile convertedProfile = new Profile(profileName,false, accountId);
        convertedProfile.setId(profileId);
        convertedProfile.setBlockedUntil(profileAdminPatch.getBlockedUntil());

        when(this.profilesRepository.getReferenceById(anyLong())).thenReturn(profileToUpdate);
        when(this.profilesRepository.save(any(ProfileJpa.class))).thenReturn(profileToUpdate);
        when(this.profileToProfileJpaConverter.convertBack(any(ProfileJpa.class))).thenReturn(convertedProfile);

        Profile savedProfile = this.adminsService.blockProfileById(profileId, profileAdminPatch);

        assertEquals(convertedProfile, savedProfile);
        assertEquals(profileAdminPatch.getBlockedUntil().toLocalDateTime(), profileToUpdate.getBlockedUntil());
        log.info("Blocked Until -> "+profileToUpdate.getBlockedUntil());
        verify(this.profilesRepository, times(1)).getReferenceById(anyLong());
        verify(this.profilesRepository, times(1)).save(any(ProfileJpa.class));
        verify(this.profileToProfileJpaConverter, times(1)).convertBack(any(ProfileJpa.class));
    }

    @Test
    void testBlockProfileById_Extend_Block_Success(){
        ProfileAdminPatch profileAdminPatch = new ProfileAdminPatch();
        profileAdminPatch.setBlockedUntil(OffsetDateTime.of(
                LocalDate.of(2023,10,10),
                LocalTime.of(12,0),
                ZoneOffset.UTC
        ));

        ProfileJpa profileToUpdate = new ProfileJpa(profileName, false, accountId);
        profileToUpdate.setCreatedAt(LocalDateTime.of(
                LocalDate.of(2023,1,1),
                LocalTime.of(12,0)
        ));
        profileToUpdate.setId(profileId);
        profileToUpdate.setBlockedUntil(LocalDateTime.of(
                LocalDate.of(2023,10,5),
                LocalTime.of(12,0)
        ));

        Profile convertedProfile = new Profile(profileName,false, accountId);
        convertedProfile.setId(profileId);
        convertedProfile.setBlockedUntil(profileAdminPatch.getBlockedUntil());

        when(this.profilesRepository.getReferenceById(anyLong())).thenReturn(profileToUpdate);
        when(this.profilesRepository.save(any(ProfileJpa.class))).thenReturn(profileToUpdate);
        when(this.profileToProfileJpaConverter.convertBack(any(ProfileJpa.class))).thenReturn(convertedProfile);

        Profile savedProfile = this.adminsService.blockProfileById(profileId, profileAdminPatch);

        assertEquals(convertedProfile, savedProfile);
        assertEquals(profileAdminPatch.getBlockedUntil().toLocalDateTime(), profileToUpdate.getBlockedUntil());
        log.info("Blocked Until -> "+profileToUpdate.getBlockedUntil());
        verify(this.profilesRepository, times(1)).getReferenceById(anyLong());
        verify(this.profilesRepository, times(1)).save(any(ProfileJpa.class));
        verify(this.profileToProfileJpaConverter, times(1)).convertBack(any(ProfileJpa.class));
    }

    @Test
    void testBlockProfileById_Unblock_Success(){
        ProfileAdminPatch profileAdminPatch = new ProfileAdminPatch();

        ProfileJpa profileToUpdate = new ProfileJpa(profileName, false, accountId);
        profileToUpdate.setCreatedAt(LocalDateTime.now());
        profileToUpdate.setId(profileId);
        profileToUpdate.setBlockedUntil(LocalDateTime.of(
                LocalDate.of(2023,10,5),
                LocalTime.of(12,0)
        ));

        Profile convertedProfile = new Profile(profileName,false, accountId);
        convertedProfile.setId(profileId);

        when(this.profilesRepository.getReferenceById(anyLong())).thenReturn(profileToUpdate);
        when(this.profilesRepository.save(any(ProfileJpa.class))).thenReturn(profileToUpdate);
        when(this.profileToProfileJpaConverter.convertBack(any(ProfileJpa.class))).thenReturn(convertedProfile);

        Profile savedProfile = this.adminsService.blockProfileById(profileId, profileAdminPatch);

        assertEquals(convertedProfile, savedProfile);
        assertNull(profileToUpdate.getBlockedUntil());
        verify(this.profilesRepository, times(1)).getReferenceById(anyLong());
        verify(this.profilesRepository, times(1)).save(any(ProfileJpa.class));
        verify(this.profileToProfileJpaConverter, times(1)).convertBack(any(ProfileJpa.class));
    }

    @Test
    void testBlockProfileById_Unblock_anotherTime_Success(){
        ProfileAdminPatch profileAdminPatch = new ProfileAdminPatch();

        ProfileJpa profileToUpdate = new ProfileJpa(profileName, false, accountId);
        profileToUpdate.setCreatedAt(LocalDateTime.now());
        profileToUpdate.setId(profileId);

        Profile convertedProfile = new Profile(profileName,false, accountId);
        convertedProfile.setId(profileId);

        when(this.profilesRepository.getReferenceById(anyLong())).thenReturn(profileToUpdate);
        when(this.profilesRepository.save(any(ProfileJpa.class))).thenReturn(profileToUpdate);
        when(this.profileToProfileJpaConverter.convertBack(any(ProfileJpa.class))).thenReturn(convertedProfile);

        Profile savedProfile = this.adminsService.blockProfileById(profileId, profileAdminPatch);

        assertEquals(convertedProfile, savedProfile);
        assertNull(profileToUpdate.getBlockedUntil());
        verify(this.profilesRepository, times(1)).getReferenceById(anyLong());
        verify(this.profilesRepository, times(1)).save(any(ProfileJpa.class));
        verify(this.profileToProfileJpaConverter, times(1)).convertBack(any(ProfileJpa.class));
    }
}
