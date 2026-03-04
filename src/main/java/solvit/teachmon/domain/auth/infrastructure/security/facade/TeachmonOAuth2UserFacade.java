package solvit.teachmon.domain.auth.infrastructure.security.facade;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import solvit.teachmon.domain.auth.infrastructure.security.strategy.OAuth2StrategyComposite;
import solvit.teachmon.domain.auth.infrastructure.security.vo.TeachmonOAuth2User;
import solvit.teachmon.domain.auth.infrastructure.security.vo.TeachmonOAuth2UserInfo;
import solvit.teachmon.domain.user.domain.entity.TeacherEntity;
import solvit.teachmon.domain.user.domain.enums.OAuth2Type;
import solvit.teachmon.domain.user.domain.repository.TeacherRepository;

@Service
@RequiredArgsConstructor
public class TeachmonOAuth2UserFacade extends DefaultOAuth2UserService {
    private final TeacherRepository teacherRepository;
    private final OAuth2StrategyComposite oAuth2StrategyComposite;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);
        OAuth2Type oAuth2Type = OAuth2Type.of(userRequest.getClientRegistration().getRegistrationId());
        TeachmonOAuth2UserInfo teachmonOAuth2UserInfo = oAuth2StrategyComposite.getOAuth2Strategy(oAuth2Type).getUserInfo(oauth2User);

        TeacherEntity teacherEntity = processTeacher(teachmonOAuth2UserInfo);

        return TeachmonOAuth2User.builder()
                .role(teacherEntity.getRole())
                .mail(teachmonOAuth2UserInfo.mail())
                .attributes(oauth2User.getAttributes())
                .build();
    }

    public TeacherEntity processTeacher(TeachmonOAuth2UserInfo teachmonOAuth2UserInfo) {
        return teacherRepository.findByMail(
                teachmonOAuth2UserInfo.mail()
        ).map(teacher -> {
            teacher.update(teachmonOAuth2UserInfo.name(), teachmonOAuth2UserInfo.profile(), teachmonOAuth2UserInfo.providerId());
            return teacher;
        }).orElseGet(() -> {
            TeacherEntity teacherEntity = TeacherEntity.builder()
                    .name(teachmonOAuth2UserInfo.name())
                    .mail(teachmonOAuth2UserInfo.mail())
                    .profile(teachmonOAuth2UserInfo.profile())
                    .providerId(teachmonOAuth2UserInfo.providerId())
                    .oAuth2Type(teachmonOAuth2UserInfo.oAuth2Type())
                    .build();
            return teacherRepository.save(teacherEntity);
        });
    }
}
