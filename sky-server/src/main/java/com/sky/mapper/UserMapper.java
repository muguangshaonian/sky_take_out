package com.sky.mapper;

import com.sky.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

@Mapper
public interface UserMapper {
    /**
     * 根据opend查询是否有这个用户
     * @param openid
     * @return
     */
    @Select("select  *from user where openid = #{openid}")
    User getByOpenId(String openid);

    /**
     * 自动注册，插入用户在表当中，
     * @param user
     */
    void insert(User user);

    @Select("select * from user where id = #{id}")
    User getById(Long userId);

    /**
     * 统计用户数量
     * @param map
     * @return
     */
    Integer countByMap(Map map);
}
