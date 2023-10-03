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
import java.util.List;

import static java.util.Arrays.asList;
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

        ProfileJpa profileToUpdate = new ProfileJpa(this.profileName, false, this.accountId);
        profileToUpdate.setCreatedAt(LocalDateTime.of(
                LocalDate.of(2023,1,1),
                LocalTime.of(12,0)
        ));
        profileToUpdate.setId(this.profileId);

        Profile convertedProfile = createProfile(this.profileName, this.accountId, this.profileId);
        convertedProfile.setBlockedUntil(profileAdminPatch.getBlockedUntil());

        when(this.profilesRepository.getReferenceById(anyLong())).thenReturn(profileToUpdate);
        when(this.profilesRepository.save(any(ProfileJpa.class))).thenReturn(profileToUpdate);
        when(this.profileToProfileJpaConverter.convertBack(any(ProfileJpa.class))).thenReturn(convertedProfile);

        Profile savedProfile = this.adminsService.blockProfileById(this.profileId, profileAdminPatch);

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

        ProfileJpa profileToUpdate = new ProfileJpa(this.profileName, false, this.accountId);
        profileToUpdate.setCreatedAt(LocalDateTime.of(
                LocalDate.of(2023,1,1),
                LocalTime.of(12,0)
        ));
        profileToUpdate.setId(this.profileId);
        profileToUpdate.setBlockedUntil(LocalDateTime.of(
                LocalDate.of(2023,10,5),
                LocalTime.of(12,0)
        ));

        Profile convertedProfile = createProfile(this.profileName, this.accountId, this.profileId);
        convertedProfile.setBlockedUntil(profileAdminPatch.getBlockedUntil());

        when(this.profilesRepository.getReferenceById(anyLong())).thenReturn(profileToUpdate);
        when(this.profilesRepository.save(any(ProfileJpa.class))).thenReturn(profileToUpdate);
        when(this.profileToProfileJpaConverter.convertBack(any(ProfileJpa.class))).thenReturn(convertedProfile);

        Profile savedProfile = this.adminsService.blockProfileById(this.profileId, profileAdminPatch);

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

        ProfileJpa profileToUpdate = new ProfileJpa(this.profileName, false, this.accountId);
        profileToUpdate.setCreatedAt(LocalDateTime.now());
        profileToUpdate.setId(this.profileId);
        profileToUpdate.setBlockedUntil(LocalDateTime.of(
                LocalDate.of(2023,10,5),
                LocalTime.of(12,0)
        ));

        Profile convertedProfile = createProfile(this.profileName, this.accountId, this.profileId);

        when(this.profilesRepository.getReferenceById(anyLong())).thenReturn(profileToUpdate);
        when(this.profilesRepository.save(any(ProfileJpa.class))).thenReturn(profileToUpdate);
        when(this.profileToProfileJpaConverter.convertBack(any(ProfileJpa.class))).thenReturn(convertedProfile);

        Profile savedProfile = this.adminsService.blockProfileById(this.profileId, profileAdminPatch);

        assertEquals(convertedProfile, savedProfile);
        assertNull(profileToUpdate.getBlockedUntil());
        verify(this.profilesRepository, times(1)).getReferenceById(anyLong());
        verify(this.profilesRepository, times(1)).save(any(ProfileJpa.class));
        verify(this.profileToProfileJpaConverter, times(1)).convertBack(any(ProfileJpa.class));
    }

    @Test
    void testBlockProfileById_Unblock_anotherTime_Success(){
        ProfileAdminPatch profileAdminPatch = new ProfileAdminPatch();

        ProfileJpa profileToUpdate = new ProfileJpa(this.profileName, false, this.accountId);
        profileToUpdate.setCreatedAt(LocalDateTime.now());
        profileToUpdate.setId(this.profileId);

        Profile convertedProfile = createProfile(this.profileName, this.accountId, this.profileId);

        when(this.profilesRepository.getReferenceById(anyLong())).thenReturn(profileToUpdate);
        when(this.profilesRepository.save(any(ProfileJpa.class))).thenReturn(profileToUpdate);
        when(this.profileToProfileJpaConverter.convertBack(any(ProfileJpa.class))).thenReturn(convertedProfile);

        Profile savedProfile = this.adminsService.blockProfileById(this.profileId, profileAdminPatch);

        assertEquals(convertedProfile, savedProfile);
        assertNull(profileToUpdate.getBlockedUntil());
        verify(this.profilesRepository, times(1)).getReferenceById(anyLong());
        verify(this.profilesRepository, times(1)).save(any(ProfileJpa.class));
        verify(this.profileToProfileJpaConverter, times(1)).convertBack(any(ProfileJpa.class));
    }

    @Test
    void testFindAllProfiles_AllProfiles_Success(){
        List<ProfileJpa> profileJpaList = createAllProfileJpaList();
        List<Profile> profileList = createAllProfileList(profileJpaList);

        when(this.profilesRepository.findAll()).thenReturn(profileJpaList);
        when(this.profileToProfileJpaConverter.convertBack(profileJpaList.get(0))).thenReturn(profileList.get(0));
        when(this.profileToProfileJpaConverter.convertBack(profileJpaList.get(1))).thenReturn(profileList.get(1));
        when(this.profileToProfileJpaConverter.convertBack(profileJpaList.get(2))).thenReturn(profileList.get(2));
        List<Profile> returnedProfileList = this.adminsService.findAllProfiles(true);

        assertEquals(profileList, returnedProfileList);
        verify(this.profilesRepository, times(1)).findAll();
        verify(this.profileToProfileJpaConverter, times(3)).convertBack(any(ProfileJpa.class));

        log.info(returnedProfileList.toString());
    }

    @Test
    void testFindAllProfiles_AllActiveProfiles_Success(){
        List<ProfileJpa> profileJpaList = createAllActiveProfileJpaList();
        List<Profile> profileList = createAllActiveProfileList(profileJpaList);

        when(this.profilesRepository.findAllActiveProfiles()).thenReturn(profileJpaList);
        when(this.profileToProfileJpaConverter.convertBack(profileJpaList.get(0))).thenReturn(profileList.get(0));
        when(this.profileToProfileJpaConverter.convertBack(profileJpaList.get(1))).thenReturn(profileList.get(1));
        List<Profile> returnedProfileList = this.adminsService.findAllProfiles(false);

        assertEquals(profileList, returnedProfileList);
        verify(this.profilesRepository, times(1)).findAllActiveProfiles();
        verify(this.profileToProfileJpaConverter, times(2)).convertBack(any(ProfileJpa.class));

        log.info(returnedProfileList.toString());
    }

    private static List<Profile> createAllProfileList(List<ProfileJpa> profileJpaList) {
        return asList(
                createProfile(profileJpaList.get(0).getProfileName(), profileJpaList.get(0).getAccountId(), profileJpaList.get(0).getId()),
                createProfile(profileJpaList.get(1).getProfileName(), profileJpaList.get(1).getAccountId(), profileJpaList.get(1).getId()),
                createProfile(profileJpaList.get(2).getProfileName(), profileJpaList.get(2).getAccountId(), profileJpaList.get(2).getId())
        );
    }

    private static List<Profile> createAllActiveProfileList(List<ProfileJpa> profileJpaList) {
        return asList(
                createProfile(profileJpaList.get(0).getProfileName(), profileJpaList.get(0).getAccountId(), profileJpaList.get(0).getId()),
                createProfile(profileJpaList.get(1).getProfileName(), profileJpaList.get(1).getAccountId(), profileJpaList.get(1).getId())
        );
    }

    private static List<ProfileJpa> createAllProfileJpaList() {
        ProfileJpa profileJpa1 = createProfileJpa("pinco_pallino", 1L, 1L, false);
        ProfileJpa profileJpa2 = createProfileJpa("marioBros", 2L, 2L, false);
        ProfileJpa profileJpa3 = createProfileJpa("zelda", 3L, 3L, true);
        return asList(profileJpa1, profileJpa2, profileJpa3);
    }

    private static List<ProfileJpa> createAllActiveProfileJpaList() {
        ProfileJpa profileJpa1 = createProfileJpa("pinco_pallino", 1L, 1L, false);
        ProfileJpa profileJpa2 = createProfileJpa("marioBros", 2L, 2L, false);
        return asList(profileJpa1, profileJpa2);
    }

    private static Profile createProfile(String profileName, Long accountId, Long profileId) {
        Profile profile1 = new Profile(profileName, false, accountId);
        profile1.setId(profileId);
        return profile1;
    }

    private static ProfileJpa createProfileJpa(String profileName, Long accountId, Long profileId, Boolean removed) {
        ProfileJpa profileJpa = new ProfileJpa(profileName,false, accountId);
        profileJpa.setId(profileId);
        if (removed){
            profileJpa.setDeletedAt(LocalDateTime.of(
                    LocalDate.of(2023,1,1),
                    LocalTime.of(12,10,30)
            ));
        }
        return profileJpa;
    }
}
