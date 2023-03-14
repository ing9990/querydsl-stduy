package study.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QMember.*;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @PersistenceContext
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    public void each() {
        queryFactory = new JPAQueryFactory(em);

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");

        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

        em.flush();
        em.clear();
    }


    @DisplayName("Member1 찾기[JPQL]")
    @Test
    void startJPQL() {
        Member findMember = em.createQuery("select m from Member m where m.username=:username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertThat(findMember).isNotNull();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }


    @DisplayName("Member1 찾기[Querydsl]")
    @Test
    void startQuerydsl() {

        Member findMember = queryFactory.selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        assertThat(findMember).isNotNull();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @DisplayName("다중 조건 쿼리")
    @Test
    void search() {
        Member findMember = queryFactory.selectFrom(member)
                .where(member.username.eq("member1")
                        .and(member.age.eq(10))
                        .and(member.team.name.eq("teamA"))
                ).fetchOne();

        assertThat(findMember).isNotNull();
        assertThat(findMember.getUsername()).isEqualTo("member1");
        assertThat(findMember.getAge()).isEqualTo(10);
        assertThat(findMember.getTeam().getName()).isEqualTo("teamA");
    }

    @DisplayName("다중 조건 쿼리2")
    @Test
    void search2() {
        List<Member> findMembers = queryFactory.selectFrom(member)
                .where(member.username.contains("member")
                        // goe -> GreaterOrEquals
                        .and(member.age.goe(10))
                        // loe -> LowerOrEquals
                        .and(member.team.members.size().loe(2)))
                .fetch();

        assertThat(findMembers.size()).isEqualTo(4);
    }

    @DisplayName("and() 간단하게 사용하기")
    @Test
    void search3() {
        Member findMember = queryFactory.selectFrom(member)
                .where(
                        member.username.eq("member1"),
                        member.age.loe(10),
                        member.team.name.eq("teamA"),
                        member.team.members.size().between(0, 50),
                        null,
                        null,
                        null
                )
                .fetchOne();

        assertThat(findMember).isNotNull();
    }

    @DisplayName("Fetch")
    @Test
    void fetchResult() {

        List<Member> fetch = queryFactory
                .selectFrom(member)
                .fetch();

        Member fetchFirst = queryFactory
                .selectFrom(member)
                .fetchFirst();

        QueryResults<Member> result = queryFactory
                .selectFrom(member)
                .fetchResults();

        result.getTotal();
        result.getLimit();
        result.getOffset();

        long totalCount = queryFactory
                .selectFrom(member)
                .fetchCount();

        assertThat(totalCount).isEqualTo(4);
    }

    @DisplayName("정렬")
    @Test
    /*
     * 회원 정렬 우선순위
     *  1. 회원 나이 내림차순
     *  2. 회원 이름 오름차순
     *  3. 회원 이름이 없으면 마지막으로 출력
     */
    void sort() {
        // 데이터 추가
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> fetch = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        var member5 = fetch.get(0);
        var member6 = fetch.get(1);
        var memberNull = fetch.get(2);

        assertThat(member5).isNotNull();
        assertThat(member6).isNotNull();
        assertThat(memberNull).isNotNull();

        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }


}















