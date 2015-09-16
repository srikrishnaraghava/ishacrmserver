package crmdna.api.endpoint;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.appengine.api.users.User;
import crmdna.common.Utils;
import crmdna.common.Utils.Currency;
import crmdna.common.api.APIResponse;
import crmdna.common.api.APIResponse.Status;
import crmdna.common.api.APIUtils;
import crmdna.common.api.RequestInfo;
import crmdna.group.Group;
import crmdna.program.Program;
import crmdna.program.ProgramProp;
import crmdna.programtype.ProgramType;
import crmdna.teacher.Teacher;
import crmdna.venue.Venue;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

import static crmdna.common.AssertUtils.ensureNotNull;

@Api(name = "program")
public class ProgramIshaApi {

    @ApiMethod(path = "createSingaporeIshaKriyaProgram", httpMethod = HttpMethod.POST)
    public APIResponse createSingaporeIshaKriyaProgram(
            @Named("venueDropDown") SingaporeIshaKriyaVenue venueEnum,
            @Nullable @Named("venueIdOrName") String venueIdOrName,
            @Named("teacherDropDown") SingaporeIshaKriyaTeacher teacherEnum,
            @Nullable @Named("teacherIdOrEmail") String teacherIdOrEmail,
            @Named("dateOfProgramYYYYMMDD") int dateYYYYMMDD,
            @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        String client = "isha";
        String login = null;

        try {

            long groupId = Group.safeGetByIdOrName(client, "isha singapore").toProp().groupId;

            long programTypeId =
                    ProgramType.safeGetByIdOrName(client, "isha kriya").toProp(client).programTypeId;

            if (venueEnum != SingaporeIshaKriyaVenue.OTHER) {
                venueIdOrName = venueEnum.getValue();
            } else {
                ensureNotNull(venueIdOrName, "venueIdOrName should be specified when venueDropDown is OTHER");
            }

            long venueId = Venue.safeGetByIdOrName(client, venueIdOrName).toProp().venueId;

            if (teacherEnum != SingaporeIshaKriyaTeacher.OTHER) {
                teacherIdOrEmail = teacherEnum.getValue();
            } else {
                ensureNotNull(teacherIdOrEmail, "teacherIdOrEmail should be specified with teacherDropDown is OTHER");
            }

            long teacherId = Teacher.safeGetByIdOrEmail(client, teacherIdOrEmail).toProp().teacherId;

            ProgramProp programProp =
                    Program.create(client, groupId, programTypeId, venueId, teacherId, dateYYYYMMDD,
                            dateYYYYMMDD, 1, null, 0.0, Currency.SGD, Utils.getLoginEmail(user));

            return new APIResponse().status(Status.SUCCESS).object(programProp);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    @ApiMethod(path = "createSingaporeSathsang", httpMethod = HttpMethod.POST)
    public APIResponse createSingaporeSathsang(
            @Named("venueDropDown") SingaporeSathsangVenue venueEnum,
            @Nullable @Named("venueIdOrName") String venueIdOrName,
            @Named("teacherDropDown") SingaporeSathsangTeacher teacherEnum,
            @Nullable @Named("teacherIdOrEmail") String teacherIdOrEmail,
            @Named("dateOfProgramYYYYMMDD") int dateYYYYMMDD,
            @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        String client = "isha";
        String login = null;

        try {

            long groupId = Group.safeGetByIdOrName(client, "isha singapore").toProp().groupId;

            long programTypeId =
                    ProgramType.safeGetByIdOrName(client, "sathsang").toProp(client).programTypeId;

            if (venueEnum != SingaporeSathsangVenue.OTHER) {
                venueIdOrName = venueEnum.getValue();
            } else {
                ensureNotNull(venueIdOrName, "venueIdOrName should be specified when venueDropDown is OTHER");
            }

            long venueId = Venue.safeGetByIdOrName(client, venueIdOrName).toProp().venueId;

            if (teacherEnum != SingaporeSathsangTeacher.OTHER) {
                teacherIdOrEmail = teacherEnum.getValue();
            } else {
                ensureNotNull(teacherIdOrEmail, "teacherIdOrEmail should be specified with teacherDropDown is OTHER");
            }

            long teacherId = Teacher.safeGetByIdOrEmail(client, teacherIdOrEmail).toProp().teacherId;

            ProgramProp programProp =
                    Program.create(client, groupId, programTypeId, venueId, teacherId, dateYYYYMMDD,
                            dateYYYYMMDD, 1, null, 0.0, Currency.SGD, Utils.getLoginEmail(user));

            return new APIResponse().status(Status.SUCCESS).object(programProp);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    @ApiMethod(path = "createSingaporeIshaUpaYogaProgram", httpMethod = HttpMethod.POST)
    public APIResponse createSingaporeIshaUpaYogaProgram(
            @Named("venueDropDown") SingaporeUpaYogaVenue venueEnum,
            @Nullable @Named("venueIdOrName") String venueIdOrName,
            @Named("teacherDropDown") SingaporeUpaYogaTeacher teacherEnum,
            @Nullable @Named("teacherIdOrEmail") String teacherIdOrEmail,
            @Named("dateOfProgramYYYYMMDD") int dateYYYYMMDD,
            @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        String client = "isha";
        String login = null;

        try {

            long groupId = Group.safeGetByIdOrName(client, "isha singapore").toProp().groupId;

            long programTypeId =
                    ProgramType.safeGetByIdOrName(client, "ishaupayoga").toProp(client).programTypeId;

            if (venueEnum != SingaporeUpaYogaVenue.OTHER) {
                venueIdOrName = venueEnum.getValue();
            } else {
                ensureNotNull(venueIdOrName, "venueIdOrName should be specified when venueDropDown is OTHER");
            }

            long venueId = Venue.safeGetByIdOrName(client, venueIdOrName).toProp().venueId;

            if (teacherEnum != SingaporeUpaYogaTeacher.OTHER) {
                teacherIdOrEmail = teacherEnum.getValue();
            } else {
                ensureNotNull(teacherIdOrEmail, "teacherIdOrEmail should be specified with teacherDropDown is OTHER");
            }

            long teacherId = Teacher.safeGetByIdOrEmail(client, teacherIdOrEmail).toProp().teacherId;

            ProgramProp programProp =
                    Program.create(client, groupId, programTypeId, venueId, teacherId, dateYYYYMMDD,
                            dateYYYYMMDD, 1, null, 0.0, Currency.SGD, Utils.getLoginEmail(user));

            return new APIResponse().status(Status.SUCCESS).object(programProp);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    @ApiMethod(path = "createSingaporeShambhavi2DayProgram", httpMethod = HttpMethod.POST)
    public APIResponse createSingaporeShambhavi2DayProgram(
            @Named("venueDropDown") SingaporeShambhavi2DayVenue venueEnum,
            @Nullable @Named("venueIdOrName") String venueIdOrName,
            @Named("teacherDropDown") ApacShambhavi2DayTeacher teacherEnum,
            @Nullable @Named("teacherIdOrEmail") String teacherIdOrEmail,
            @Named("dateOfProgramYYYYMMDD") int dateYYYYMMDD,
            @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        String client = "isha";
        String login = null;

        try {

            long groupId = Group.safeGetByIdOrName(client, "isha singapore").toProp().groupId;

            long programTypeId =
                    ProgramType.safeGetByIdOrName(client, "shambhavi2day").toProp(client).programTypeId;

            if (venueEnum != SingaporeShambhavi2DayVenue.OTHER) {
                venueIdOrName = venueEnum.getValue();
            } else {
                ensureNotNull(venueIdOrName, "venueIdOrName should be specified when venueDropDown is OTHER");
            }

            long venueId = Venue.safeGetByIdOrName(client, venueIdOrName).toProp().venueId;

            if (teacherEnum != ApacShambhavi2DayTeacher.OTHER) {
                teacherIdOrEmail = teacherEnum.getValue();
            } else {
                ensureNotNull(teacherIdOrEmail, "teacherIdOrEmail should be specified with teacherDropDown is OTHER");
            }

            long teacherId = Teacher.safeGetByIdOrEmail(client, teacherIdOrEmail).toProp().teacherId;

            double fees = 200.0;
            ProgramProp programProp =
                    Program.create(client, groupId, programTypeId, venueId, teacherId, dateYYYYMMDD,
                            dateYYYYMMDD, 1, null, fees, Currency.SGD, Utils.getLoginEmail(user));

            return new APIResponse().status(Status.SUCCESS).object(programProp);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    public enum IshaProgramType {
        ANGAMARDANA, BHAVA_SPANDANA, BHUTA_SHUDDHI, GURU_POOJA, HATA_YOGA_21_DAY, HATA_YOGA_3_DAY, INNER_ENGINEERING_7_DAY, INNER_ENGINEERING_ONLINE, INNER_ENGINEERING_RETREAT, INNER_ENGINEERING_4_DAY, ISHA_KRIYA, ISHA_YOGA_7_DAY, ISHA_YOGA_FOR_CHILDREN, MYSTIC_EYE, PUBLIC_TALK, PUBLIC_TALK_ISHA_KRIYA, SAHAJA_STHITHI_YOGA, SATHSANG, SHAMBHAVI_2_DAY, SURYA_KRIYA, UPA_YOGA, WHOLENESS, YOGA_ASANAS, SHOONYA_INTENSIVE, SAMYAMA_8_DAY
    }

    public enum GroupEnum {
        ISHA_SINGAPORE, BHAIRAVI_YOGA, ISHA_KL, ISHA_PENANG, ISHA_SYDNEY, ISHA_MELBOURNE, OTHER
    }

    public enum SingaporeIshaKriyaVenue {
        WOODLANDS_CC("woodlands cc"), YUHUA_CC("yuhua cc"), BHAIRAVI_YOGA_STUDIO("bhairavi yoga studio"), OTHER("other");

        private final String value;

        private SingaporeIshaKriyaVenue(final String value) {
            this.value = value;
        }
        public String getValue() {
            return value;
        }
    }

    public enum SingaporeSathsangVenue {
        SINGAI_TAMIL_SANGAM("singaitamilsangam"), OTHER("other");

        private final String value;

        private SingaporeSathsangVenue(final String value) {
            this.value = value;
        }
        public String getValue() {
            return value;
        }
    }

    public enum SingaporeUpaYogaVenue {
        APERIA_MALL("aperiamall"), OTHER("other");

        private final String value;

        private SingaporeUpaYogaVenue(final String value) {
            this.value = value;
        }
        public String getValue() {
            return value;
        }
    }

    public enum SingaporeShambhavi2DayVenue {
        NPS_SCHOOL("npsinternationalschool"), OTHER("other");

        private final String value;

        private SingaporeShambhavi2DayVenue(final String value) {
            this.value = value;
        }
        public String getValue() {
            return value;
        }
    }

    public enum SingaporeIshaKriyaTeacher {
        SHAMLA("tshamn@gmail.com"),
        BAK_LIANG_LOR("lorbakliang@gmail.com"), SUNDARA_VADIVEL("vsundar18@gmail.com"),
        JIANWEN("t0iddii@yahoo.com"), SHILPI_MALHOTRA("smalhot1@googlemail.com"), SWAMINATHAN("imswaminathan@gmail.com"), OTHER("other");

        private final String value;
        SingaporeIshaKriyaTeacher(String value) {
            this.value = value;
        }

        String getValue() {
            return value;
        }
    }

    public enum ApacShambhavi2DayTeacher {
        NIDHI_JAIN("nidhi.jain@ishafoundation.org"),
        SAURABH_JAIN("saurabh.jain@ishafoundation.org"), OTHER("other");

        private final String value;
        ApacShambhavi2DayTeacher(String value) {
            this.value = value;
        }

        String getValue() {
            return value;
        }
    }

    public enum SingaporeUpaYogaTeacher {
        JIANWEN("t0iddii@yahoo.com"), SHILPI_MALHOTRA("smalhot1@googlemail.com"), OTHER("other");

        private final String value;
        SingaporeUpaYogaTeacher(String value) {
            this.value = value;
        }

        String getValue() {
            return value;
        }
    }

    public enum SingaporeSathsangTeacher {
        SATHYA_THILAKAN("sathya.t@ishafoundation.org"),
        THULASIDHAR_KOSALRAM("thulasidhar@gmail.com"), NARASIMHAN("l.narasimhan.d@gmail.com"),
        VANITHA_VISWESWARAN("vanithavisweswaran@gmail.com"), OTHER("other");

        private final String value;
        SingaporeSathsangTeacher(String value) {
            this.value = value;
        }

        String getValue() {
            return value;
        }
    }
}
