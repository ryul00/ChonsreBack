package TourCompetition.ChonsreBack.Domain.Func.Entitiy;

import TourCompetition.ChonsreBack.Domain.Kakao.Entity.KakaoUser;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "Course_Recommend_Group")
public class RecommendGroup {
    public enum InpCourseStyle {
        farm,
        fishing,
        etc
    }
    @Id
    @Column(nullable = false, unique = true)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long groupId;
    private  String inpStartDate;
    private String inpEndDate;
//    private String inpRegion;


    private Integer inpAdultCnt;

    private Integer inpChildCnt;

    private Integer inpBabyCnt;

    @Transient
    public Integer getTotalPeople() {
        return (inpAdultCnt == null ? 0 : inpAdultCnt)
                + (inpChildCnt == null ? 0 : inpChildCnt)
                + (inpBabyCnt == null ? 0 : inpBabyCnt);
    }


    @Enumerated(EnumType.STRING)
    private InpCourseStyle inpStyle;

    private  String ReqCreatedAt;

    @ManyToOne
    @JoinColumn(name = "kakaoId", nullable = true) // 비회원은 Null
    private KakaoUser kakaoUser;

    @OneToMany(mappedBy = "recommendGroup", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Course> courseList = new ArrayList<>();

}
