package com.sergiostefanizzi.profilemicroservice.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sergiostefanizzi.profilemicroservice.model.Alert;
import com.sergiostefanizzi.profilemicroservice.model.AlertPatch;
import com.sergiostefanizzi.profilemicroservice.model.Profile;
import com.sergiostefanizzi.profilemicroservice.model.ProfileAdminPatch;
import com.sergiostefanizzi.profilemicroservice.repository.AlertsRepository;
import com.sergiostefanizzi.profilemicroservice.repository.CommentsRepository;
import com.sergiostefanizzi.profilemicroservice.repository.PostsRepository;
import com.sergiostefanizzi.profilemicroservice.repository.ProfilesRepository;
import com.sergiostefanizzi.profilemicroservice.service.AdminsService;
import com.sergiostefanizzi.profilemicroservice.system.exception.AlertNotFoundException;
import com.sergiostefanizzi.profilemicroservice.system.exception.AlertStatusNotValidException;
import com.sergiostefanizzi.profilemicroservice.system.exception.ProfileNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.*;
import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(AdminsController.class)
@ActiveProfiles("test")
@Slf4j
public class AdminsControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private AdminsService adminsService;
    @MockBean
    private ProfilesRepository profilesRepository;
    @MockBean
    private PostsRepository postsRepository;
    @MockBean
    private CommentsRepository commentsRepository;
    @MockBean
    private AlertsRepository alertsRepository;
    @Autowired
    private ObjectMapper objectMapper;
    private Long profileId = 1L;
    private Long accountId = 1L;
    private Long alertId = 1L;
    private Long alertOwnerId = 1L;
    private Long alertPostId = 1L;
    private Long managedById = 2L;
    private String alertReason = "Motivo della segnalazione";
    private String profileName = "pinco_pallino";
    private OffsetDateTime blockedUntilTime = OffsetDateTime.of(
            LocalDate.of(2024,10,5),
            LocalTime.of(12,15,30),
            ZoneOffset.UTC
    );

    private LocalDateTime createdAtTime = LocalDateTime.of(
            LocalDate.of(2023,1,1),
            LocalTime.of(12,0,0)
    );
    @BeforeEach
    void setUp() {

    }

    @AfterEach
    void tearDown() {

    }

    @Test
    void testBlockProfileById_Block_Then_200() throws Exception {
        Profile updatedProfile = new Profile(this.profileName,false, this.accountId);
        updatedProfile.setId(this.profileId);
        updatedProfile.blockedUntil(this.blockedUntilTime);

        ProfileAdminPatch profileAdminPatch = new ProfileAdminPatch();
        profileAdminPatch.setBlockedUntil(this.blockedUntilTime);

        when(this.profilesRepository.adminCheckActiveById(anyLong())).thenReturn(Optional.of(profileId));
        when(this.adminsService.blockProfileById(anyLong(), any(ProfileAdminPatch.class))).thenReturn(updatedProfile);

        String profileAdminPatchJson = this.objectMapper.writeValueAsString(profileAdminPatch);

        MvcResult result = this.mockMvc.perform(patch("/admins/profiles/{profileId}",profileId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(profileAdminPatchJson)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.*", hasSize(5)))
                .andExpect(jsonPath("$.id").value(updatedProfile.getId()))
                .andExpect(jsonPath("$.profile_name").value(updatedProfile.getProfileName()))
                .andExpect(jsonPath("$.is_private").value(updatedProfile.getIsPrivate()))
                .andExpect(jsonPath("$.blocked_until").value(updatedProfile.getBlockedUntil().toString()))
                .andExpect(jsonPath("$.account_id").value(updatedProfile.getAccountId()))
                .andReturn();

        // salvo risposta in result per visualizzarla
        String resultAsString = result.getResponse().getContentAsString();
        Profile profileResult = this.objectMapper.readValue(resultAsString, Profile.class);

        log.info(profileResult.toString());
    }

    @Test
    void testBlockProfileById_Unblock_Then_200() throws Exception {
        Profile updatedProfile = new Profile(this.profileName,false, this.accountId);
        updatedProfile.setId(this.profileId);

        ProfileAdminPatch profileAdminPatch = new ProfileAdminPatch();


        when(this.profilesRepository.adminCheckActiveById(anyLong())).thenReturn(Optional.of(profileId));
        when(this.adminsService.blockProfileById(anyLong(), any(ProfileAdminPatch.class))).thenReturn(updatedProfile);

        String profileAdminPatchJson = this.objectMapper.writeValueAsString(profileAdminPatch);

        MvcResult result = this.mockMvc.perform(patch("/admins/profiles/{profileId}",profileId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(profileAdminPatchJson)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.*", hasSize(4)))
                .andExpect(jsonPath("$.id").value(updatedProfile.getId()))
                .andExpect(jsonPath("$.profile_name").value(updatedProfile.getProfileName()))
                .andExpect(jsonPath("$.is_private").value(updatedProfile.getIsPrivate()))
                .andExpect(jsonPath("$.account_id").value(updatedProfile.getAccountId()))
                .andReturn();

        // salvo risposta in result per visualizzarla
        String resultAsString = result.getResponse().getContentAsString();
        Profile profileResult = this.objectMapper.readValue(resultAsString, Profile.class);

        log.info(profileResult.toString());
    }

    @Test
    void testBlockProfileById_Block_MissingBody_Then_400() throws Exception {
        Profile updatedProfile = new Profile(this.profileName,false, this.accountId);
        updatedProfile.setId(this.profileId);
        updatedProfile.blockedUntil(this.blockedUntilTime);


        when(this.profilesRepository.adminCheckActiveById(anyLong())).thenReturn(Optional.of(profileId));
        when(this.adminsService.blockProfileById(anyLong(), any(ProfileAdminPatch.class))).thenReturn(updatedProfile);

        MvcResult result = this.mockMvc.perform(patch("/admins/profiles/{profileId}",profileId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isBadRequest())
                .andExpect(res -> assertTrue(res.getResolvedException() instanceof HttpMessageNotReadableException))
                .andExpect(jsonPath("$.error").value("Required request body is missing: public org.springframework.http.ResponseEntity<com.sergiostefanizzi.profilemicroservice.model.Profile> com.sergiostefanizzi.profilemicroservice.controller.AdminsController.blockProfileById(java.lang.Long,com.sergiostefanizzi.profilemicroservice.model.ProfileAdminPatch)"))
                .andReturn();

        // salvo risposta in result per visualizzarla
        String resultAsString = result.getResponse().getContentAsString();
        log.info("\nErrors\n" +resultAsString);
    }

    @Test
    void testBlockProfileById_Block_PastDate_Then_400() throws Exception {
        Profile updatedProfile = new Profile(this.profileName,false, this.accountId);
        updatedProfile.setId(this.profileId);
        updatedProfile.blockedUntil(this.blockedUntilTime);

        ProfileAdminPatch profileAdminPatch = new ProfileAdminPatch();
        profileAdminPatch.setBlockedUntil(this.blockedUntilTime.minusYears(2));

        when(this.profilesRepository.adminCheckActiveById(anyLong())).thenReturn(Optional.of(profileId));
        when(this.adminsService.blockProfileById(anyLong(), any(ProfileAdminPatch.class))).thenReturn(updatedProfile);

        String profileAdminPatchJson = this.objectMapper.writeValueAsString(profileAdminPatch);

        MvcResult result = this.mockMvc.perform(patch("/admins/profiles/{profileId}",profileId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(profileAdminPatchJson)
                )
                .andExpect(status().isBadRequest())
                .andExpect(res -> assertTrue(res.getResolvedException() instanceof MethodArgumentNotValidException))
                .andExpect(jsonPath("$.error").value("blockedUntil must be a future date"))
                .andReturn();

        // salvo risposta in result per visualizzarla
        String resultAsString = result.getResponse().getContentAsString();
        log.info("\nErrors\n" +resultAsString);
    }

    @Test
    void testBlockProfileById_Block_IdNotLong_Then_400() throws Exception {
        Profile updatedProfile = new Profile(this.profileName,false, this.accountId);
        updatedProfile.setId(this.profileId);
        updatedProfile.blockedUntil(this.blockedUntilTime);

        ProfileAdminPatch profileAdminPatch = new ProfileAdminPatch();
        profileAdminPatch.setBlockedUntil(this.blockedUntilTime);

        when(this.profilesRepository.adminCheckActiveById(anyLong())).thenReturn(Optional.empty());
        when(this.adminsService.blockProfileById(anyLong(), any(ProfileAdminPatch.class))).thenReturn(updatedProfile);

        String profileAdminPatchJson = this.objectMapper.writeValueAsString(profileAdminPatch);

        MvcResult result = this.mockMvc.perform(patch("/admins/profiles/{profileId}","IdNotLong")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(profileAdminPatchJson)
                )
                .andExpect(status().isBadRequest())
                .andExpect(res -> assertTrue(res.getResolvedException() instanceof NumberFormatException))
                .andExpect(jsonPath("$.error").value("ID is not valid!"))
                .andReturn();

        // salvo risposta in result per visualizzarla
        String resultAsString = result.getResponse().getContentAsString();
        log.info("\nErrors\n" +resultAsString);
    }

    @Test
    void testBlockProfileById_Block_DateNotReadable_Then_400() throws Exception {
        Profile updatedProfile = new Profile(this.profileName,false, this.accountId);
        updatedProfile.setId(this.profileId);
        updatedProfile.blockedUntil(this.blockedUntilTime);

        ProfileAdminPatch profileAdminPatch = new ProfileAdminPatch();
        profileAdminPatch.setBlockedUntil(this.blockedUntilTime);

        when(this.profilesRepository.adminCheckActiveById(anyLong())).thenReturn(Optional.of(profileId));
        when(this.adminsService.blockProfileById(anyLong(), any(ProfileAdminPatch.class))).thenReturn(updatedProfile);

        String profileAdminPatchJson = this.objectMapper.writeValueAsString(profileAdminPatch);
        JsonNode jsonNode = this.objectMapper.readTree(profileAdminPatchJson);

        ((ObjectNode) jsonNode).put("blocked_until", "NotValidDate");
        profileAdminPatchJson = this.objectMapper.writeValueAsString(jsonNode);

        MvcResult result = this.mockMvc.perform(patch("/admins/profiles/{profileId}",profileId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(profileAdminPatchJson)
                )
                .andExpect(status().isBadRequest())
                .andExpect(res -> assertTrue(res.getResolvedException() instanceof HttpMessageNotReadableException))
                .andExpect(jsonPath("$.error").value("JSON parse error: Cannot deserialize value of type `java.time.OffsetDateTime` from String \"NotValidDate\": Failed to deserialize java.time.OffsetDateTime: (java.time.format.DateTimeParseException) Text 'NotValidDate' could not be parsed at index 0"))
                .andReturn();

        // salvo risposta in result per visualizzarla
        String resultAsString = result.getResponse().getContentAsString();
        log.info("\nErrors\n" +resultAsString);
    }

    @Test
    void testBlockProfileById_Block_ProfileNotFound_Then_404() throws Exception {
        Profile updatedProfile = new Profile(this.profileName,false, this.accountId);
        updatedProfile.setId(this.profileId);
        updatedProfile.blockedUntil(this.blockedUntilTime);

        ProfileAdminPatch profileAdminPatch = new ProfileAdminPatch();
        profileAdminPatch.setBlockedUntil(this.blockedUntilTime);

        when(this.profilesRepository.adminCheckActiveById(anyLong())).thenReturn(Optional.empty());
        when(this.adminsService.blockProfileById(anyLong(), any(ProfileAdminPatch.class))).thenReturn(updatedProfile);

        String profileAdminPatchJson = this.objectMapper.writeValueAsString(profileAdminPatch);

        MvcResult result = this.mockMvc.perform(patch("/admins/profiles/{profileId}",profileId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(profileAdminPatchJson)
                )
                .andExpect(status().isNotFound())
                .andExpect(res -> assertTrue(res.getResolvedException() instanceof ProfileNotFoundException))
                .andExpect(jsonPath("$.error").value("Profile "+profileId+" not found!"))
                .andReturn();

        // salvo risposta in result per visualizzarla
        String resultAsString = result.getResponse().getContentAsString();
        log.info("\nErrors\n" +resultAsString);
    }

    @Test
    void testFindAllProfiles_Then_200() throws Exception{
        List<Profile> profileList = createProfileList();

        when(this.adminsService.findAllProfiles(anyBoolean())).thenReturn(profileList);

        MvcResult result = this.mockMvc.perform(get("/admins/profiles?removedProfile={removedProfile}",true)
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].id").value(profileList.get(0).getId()))
                .andExpect(jsonPath("$[1].id").value(profileList.get(1).getId()))
                .andExpect(jsonPath("$[2].id").value(profileList.get(2).getId()))
                .andReturn();

        // salvo risposta in result per visualizzarla
        String resultAsString = result.getResponse().getContentAsString();

        log.info(resultAsString);
    }

    @Test
    void testFindAllProfiles_Then_400() throws Exception{
        List<Profile> profileList = createProfileList();

        when(this.adminsService.findAllProfiles(anyBoolean())).thenReturn(profileList);

        MvcResult result = this.mockMvc.perform(get("/admins/profiles?removedProfile={removedProfile}","NotBoolean")
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isBadRequest())
                .andExpect(res -> assertTrue(res.getResolvedException() instanceof MethodArgumentTypeMismatchException))
                .andExpect(jsonPath("$.error").value("Failed to convert value of type 'java.lang.String' to required type 'java.lang.Boolean'; Invalid boolean value [NotBoolean]"))
                .andReturn();

        // salvo risposta in result per visualizzarla
        String resultAsString = result.getResponse().getContentAsString();
        log.info("\nErrors\n" +resultAsString);
    }

    @Test
    void testFindAlertById_Then_200() throws Exception {
        Alert returnedAlert = createAlert(1L);


        when(this.alertsRepository.checkAlertById(anyLong())).thenReturn(Optional.of(this.alertId));
        when(this.adminsService.findAlertById(anyLong())).thenReturn(returnedAlert);

        MvcResult result = this.mockMvc.perform(get("/admins/alerts/{alertId}",this.alertId)
                        .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.*", hasSize(5)))
                .andExpect(jsonPath("$.id").value(returnedAlert.getId()))
                .andExpect(jsonPath("$.created_by").value(returnedAlert.getCreatedBy()))
                .andExpect(jsonPath("$.post_id").value(returnedAlert.getPostId()))
                .andExpect(jsonPath("$.reason").value(returnedAlert.getReason()))
                .andExpect(jsonPath("$.managed_by").value(returnedAlert.getManagedBy()))
                .andReturn();

        // salvo risposta in result per visualizzarla
        String resultAsString = result.getResponse().getContentAsString();
        Alert alertResult = this.objectMapper.readValue(resultAsString, Alert.class);

        log.info(alertResult.toString());
    }

    @Test
    void testFindAlertById_IdNotLong_Then_400() throws Exception {
        MvcResult result = this.mockMvc.perform(get("/admins/alerts/IdNotLong"))
                .andExpect(status().isBadRequest())
                .andExpect(res -> assertTrue(
                        res.getResolvedException() instanceof NumberFormatException
                ))
                .andExpect(jsonPath("$.error").value("ID is not valid!")).andReturn();
        // Visualizzo l'errore
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n" + resultAsString);
        log.info("Resolved Error ---> " + result.getResolvedException());
    }

    @Test
    void testFindAlertById_NotFound_Then_404() throws Exception {
        when(this.alertsRepository.checkAlertById(anyLong())).thenReturn(Optional.empty());
        MvcResult result = this.mockMvc.perform(get("/admins/alerts/{alertId}",Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(res -> assertTrue(
                        res.getResolvedException() instanceof AlertNotFoundException
                ))
                .andExpect(jsonPath("$.error").value("Alert "+Long.MAX_VALUE+" not found!")).andReturn();
        // Visualizzo l'errore
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n" + resultAsString);
        log.info("Resolved Error ---> " + result.getResolvedException());
    }

    @Test
    void testUpdateAlertById_Then_200() throws Exception {
        Alert alert = createAlert(2L);

        String alertPatchJson = this.objectMapper.writeValueAsString(new AlertPatch(this.managedById));

        when(this.alertsRepository.checkAlertById(anyLong())).thenReturn(Optional.of(this.alertId));
        when(this.adminsService.updateAlertById(anyLong(), any(AlertPatch.class))).thenReturn(alert);

        MvcResult result = this.mockMvc.perform(patch("/admins/alerts/{alertId}",this.alertId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(alertPatchJson)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.*", hasSize(5)))
                .andExpect(jsonPath("$.id").value(alert.getId()))
                .andExpect(jsonPath("$.created_by").value(alert.getCreatedBy()))
                .andExpect(jsonPath("$.post_id").value(alert.getPostId()))
                .andExpect(jsonPath("$.reason").value(alert.getReason()))
                .andExpect(jsonPath("$.managed_by").value(alert.getManagedBy()))
                .andReturn();

        // salvo risposta in result per visualizzarla
        String resultAsString = result.getResponse().getContentAsString();
        Alert alertResult = this.objectMapper.readValue(resultAsString, Alert.class);

        log.info(alertResult.toString());
    }

    @Test
    void testUpdateAlertById_IdNotLong_Then_400() throws Exception {
        String alertPatchJson = this.objectMapper.writeValueAsString(new AlertPatch(this.managedById));
        when(this.alertsRepository.checkAlertById(anyLong())).thenReturn(Optional.empty());
        MvcResult result = this.mockMvc.perform(patch("/admins/alerts/IdNotLong")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(alertPatchJson))
                .andExpect(status().isBadRequest())
                .andExpect(res -> assertTrue(
                        res.getResolvedException() instanceof NumberFormatException
                ))
                .andExpect(jsonPath("$.error").value("ID is not valid!")).andReturn();
        // Visualizzo l'errore
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n" + resultAsString);
        log.info("Resolved Error ---> " + result.getResolvedException());
    }

    @Test
    void testUpdateAlertById_ManagedByNotLong_Then_400() throws Exception {
        String alertPatchJson = this.objectMapper.writeValueAsString(new AlertPatch(this.managedById));

        JsonNode jsonNode = this.objectMapper.readTree(alertPatchJson);
        ((ObjectNode) jsonNode).put("managed_by", "IdNotLong");
        alertPatchJson = this.objectMapper.writeValueAsString(jsonNode);

        when(this.alertsRepository.checkAlertById(anyLong())).thenReturn(Optional.of(this.alertId));
        MvcResult result = this.mockMvc.perform(patch("/admins/alerts/{alertId}",this.alertId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(alertPatchJson))
                .andExpect(status().isBadRequest())
                .andExpect(res -> assertTrue(
                        res.getResolvedException() instanceof HttpMessageNotReadableException
                ))
                .andExpect(jsonPath("$.error").value("JSON parse error: Cannot deserialize value of type `java.lang.Long` from String \"IdNotLong\": not a valid `java.lang.Long` value")).andReturn();
        // Visualizzo l'errore
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n" + resultAsString);
        log.info("Resolved Error ---> " + result.getResolvedException());
    }

    @Test
    void testUpdateAlertById_AlertNotFound_Then_404() throws Exception {

        String alertPatchJson = this.objectMapper.writeValueAsString(new AlertPatch(this.managedById));
        when(this.alertsRepository.checkAlertById(anyLong())).thenReturn(Optional.empty());
        MvcResult result = this.mockMvc.perform(patch("/admins/alerts/{alertId}",Long.MAX_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(alertPatchJson))
                .andExpect(status().isNotFound())
                .andExpect(res -> assertTrue(
                        res.getResolvedException() instanceof AlertNotFoundException
                ))
                .andExpect(jsonPath("$.error").value("Alert "+Long.MAX_VALUE+" not found!")).andReturn();
        // Visualizzo l'errore
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n" + resultAsString);
        log.info("Resolved Error ---> " + result.getResolvedException());
    }

    @Test
    void testFindAllAlerts_AllAlerts_Then_200() throws Exception{
        List<Alert> alertList = asList(
                createAlert(3L),
                createAlert(4L),
                createAlert(5L)
        );

        when(this.adminsService.findAllAlerts(anyString())).thenReturn(alertList);

        MvcResult result = this.mockMvc.perform(get("/admins/alerts?alertStatus={alertStatus}",nullValue())
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.*", hasSize(3)))
                .andExpect(jsonPath("$[0].id").value(alertList.get(0).getId()))
                .andExpect(jsonPath("$[1].id").value(alertList.get(1).getId()))
                .andExpect(jsonPath("$[2].id").value(alertList.get(2).getId()))
                .andReturn();

        // salvo risposta in result per visualizzarla
        String resultAsString = result.getResponse().getContentAsString();

        log.info(resultAsString);
    }

    @Test
    void testFindAllAlerts_AllOpenAlerts_Then_200() throws Exception{
        List<Alert> alertList = asList(
                createAlert(3L),
                createAlert(4L),
                createAlert(5L)
        );

        when(this.adminsService.findAllAlerts(anyString())).thenReturn(alertList);

        MvcResult result = this.mockMvc.perform(get("/admins/alerts?alertStatus={alertStatus}","O")
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.*", hasSize(3)))
                .andExpect(jsonPath("$[0].id").value(alertList.get(0).getId()))
                .andExpect(jsonPath("$[1].id").value(alertList.get(1).getId()))
                .andExpect(jsonPath("$[2].id").value(alertList.get(2).getId()))
                .andReturn();

        // salvo risposta in result per visualizzarla
        String resultAsString = result.getResponse().getContentAsString();

        log.info(resultAsString);
    }

    @Test
    void testFindAllAlerts_AlertStatusNotValid_Then_400() throws Exception{
        when(this.adminsService.findAllAlerts(anyString())).thenThrow(AlertStatusNotValidException.class);

        MvcResult result = this.mockMvc.perform(get("/admins/alerts?alertStatus={alertStatus}","xx")
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isBadRequest())
                .andExpect(res -> assertTrue(res.getResolvedException() instanceof AlertStatusNotValidException))
                .andExpect(jsonPath("$.error").value("Alert status not valid!"))
                .andReturn();

        // salvo risposta in result per visualizzarla
        String resultAsString = result.getResponse().getContentAsString();
        log.info("\nErrors\n" +resultAsString);
    }

    private Alert createAlert(Long id) {
        Alert alert = new Alert(id, this.alertReason);
        alert.setId(id*10);
        alert.setPostId(id*100);
        alert.setManagedBy(id*1000);
        return alert;
    }



    private static List<Profile> createProfileList() {
        Profile profile1 = createProfile("pinco",1L);
        Profile profile2 = createProfile("pinco2",2L);
        Profile profile3 = createProfile("pinco3",3L);

        return asList(profile1,profile2,profile3);
    }

    private static Profile createProfile(String profileName, Long id) {
        //accountId e profileId in questo caso sono uguali
        Profile profile = new Profile(profileName,false, id);
        profile.setId(id);
        return profile;
    }
}
