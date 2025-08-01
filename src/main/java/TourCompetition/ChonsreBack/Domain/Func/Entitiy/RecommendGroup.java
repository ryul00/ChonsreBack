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
        가족여행,
        힐링여행,
        우정여행,
        데이트,
        뚜벅이,
        그외
    }
    @Id
    @Column(nullable = false, unique = true)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long groupId;
    private  String inpStartDate;
    private String inpEndDate;
    private Integer inpPeopleCnt;
    private String inpRegion;

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
