package solvit.teachmon.domain.user.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import solvit.teachmon.domain.user.domain.enums.OAuth2Type;
import solvit.teachmon.domain.user.domain.enums.Role;
import solvit.teachmon.domain.user.exception.InvalidTeacherInfoException;
import org.springframework.http.HttpStatus;
import solvit.teachmon.domain.management.teacher.domain.entity.SupervisionBanDayEntity;
import solvit.teachmon.domain.user.exception.TeacherInvalidValueException;
import solvit.teachmon.global.entity.BaseEntity;
import solvit.teachmon.domain.after_school.domain.entity.AfterSchoolEntity;
import solvit.teachmon.domain.student_schedule.domain.entity.ExitEntity;
import solvit.teachmon.domain.student_schedule.domain.entity.AwayEntity;
import solvit.teachmon.domain.supervision.domain.entity.SupervisionScheduleEntity;
import solvit.teachmon.domain.leave_seat.domain.entity.LeaveSeatEntity;
import solvit.teachmon.domain.leave_seat.domain.entity.FixedLeaveSeatEntity;

import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(
        name = "teacher",
        uniqueConstraints = @UniqueConstraint(columnNames = {"provider_id", "oauth2_type"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeacherEntity extends BaseEntity {
    private static final List<Role> STUDENT_SCHEDULE_CHANGE_AUTHORITIES = List.of(
            Role.TEACHER,
            Role.ADMIN
    );

    @Column(name = "mail", nullable = false, updatable = false)
    private String mail;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "profile")
    private String profile;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "provider_id", updatable = false)
    private String providerId;

    @Column(name = "oauth2_type", updatable = false)
    @Enumerated(EnumType.STRING)
    private OAuth2Type oAuth2Type;

    @OneToMany(mappedBy = "teacher", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<SupervisionBanDayEntity> supervisionBanDays = new ArrayList<>();

    @OneToMany(mappedBy = "teacher", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<AfterSchoolEntity> afterSchools = new ArrayList<>();

    @OneToMany(mappedBy = "teacher", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<ExitEntity> exits = new ArrayList<>();

    @OneToMany(mappedBy = "teacher", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<AwayEntity> aways = new ArrayList<>();

    @OneToMany(mappedBy = "teacher", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<SupervisionScheduleEntity> supervisionSchedules = new ArrayList<>();

    @OneToMany(mappedBy = "teacher", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<LeaveSeatEntity> leaveSeats = new ArrayList<>();

    @OneToMany(mappedBy = "teacher", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<FixedLeaveSeatEntity> fixedLeaveSeats = new ArrayList<>();

    @Builder
    public TeacherEntity(String mail, String name, String profile, String providerId, OAuth2Type oAuth2Type) {
        validateMail(mail);
        validateName(name);

        this.mail = mail;
        this.name = name;
        this.profile = profile;
        this.role = Role.TEACHER;
        this.isActive = true;
        this.providerId = providerId;
        this.oAuth2Type = oAuth2Type;
    }

    private void validateMail(String mail) {
        if(mail == null || mail.trim().isEmpty())
            throw new InvalidTeacherInfoException("메일은 비어 있을 수 없습니다.");
    }

    private void validateName(String name) {
        if(name == null || name.trim().isEmpty())
            throw new InvalidTeacherInfoException("이름은 비어 있을 수 없습니다.");
    }

    private void validateProfile(String profile) {
        if(profile == null || profile.trim().isEmpty())
            throw new InvalidTeacherInfoException("프로필은 비어 있을 수 없습니다.");
    }

    public void changeRole(Role role) {
        if(role == null) {
            throw new TeacherInvalidValueException("role(권한)은 필수입니다.", HttpStatus.BAD_REQUEST);
        }
        this.role = role;
    }

    public void changeName(String name) {
        if(name == null) {
            throw new TeacherInvalidValueException("name(이름)은 필수입니다", HttpStatus.BAD_REQUEST);
        }
        this.name = name;
    }

    public void update(String name, String profile) {
        validateName(name);
        validateProfile(profile);

        this.name = name;
        this.profile = profile;
    }

    public Boolean hasStudentScheduleChangeAuthority() {
        return this.role.isContains(STUDENT_SCHEDULE_CHANGE_AUTHORITIES);
    }
}
