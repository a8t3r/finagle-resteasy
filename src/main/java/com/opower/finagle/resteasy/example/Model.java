package com.opower.finagle.resteasy.example;

import com.google.common.base.Objects;

import java.util.List;

/**
 * @author Alexandr Kolosov
 * @since 14.11.2014
 */
public class Model {

    private String name;
    private Integer age;
    private List<Model> friends;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public List<Model> getFriends() {
        return friends;
    }

    public void setFriends(List<Model> friends) {
        this.friends = friends;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("name", name)
                .add("age", age)
                .add("friends", friends)
                .toString();
    }
}
