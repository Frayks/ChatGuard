package com.andrew.project.chatGuard.api.property;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Setter
@Getter
@Configuration
@PropertySource(value = "${translations.location}/translations.properties", encoding = "UTF-8")
@ConfigurationProperties(prefix = "translations")
public class Translations {
    private String buttonText;
    private String userWelcomeMsg;
    private String userCongratulationsMsg;
    private String settingsInfoMsg;
    private String provided;
    private String notProvided;
    private String settingsChangedMsg;
    private String wrongFormatBotCommandMsg;
    private String welcomeMsg;
    private String lostAdministratorRightsMsg;
    private String enoughRightsMsg;
    private String notEnoughRightsMsg;
    private String privateChatMsg;
    private String animationFileId;
    private String userBannedMsg;

}
