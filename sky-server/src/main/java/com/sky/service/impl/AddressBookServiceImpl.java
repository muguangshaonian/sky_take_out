package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.entity.AddressBook;
import com.sky.mapper.AddressBookMapper;
import com.sky.service.AddressBookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AddressBookServiceImpl implements AddressBookService {

    @Autowired
    private AddressBookMapper addressBookMapper;

    /**
     * 查询当前登录用户的所有地址信息
     *
     * @return
     */
    public List<AddressBook> list(AddressBook addressBook) {
        List<AddressBook> list = addressBookMapper.list(addressBook);
        return list;
    }

    /**
     * 新增地址
     *
     * @param addressBook
     */
    public void save(AddressBook addressBook) {
        addressBook.setUserId(BaseContext.getCurrentId());
        addressBook.setIsDefault(0);
        addressBookMapper.save(addressBook);
    }

    /**
     * 根据id查询地址
     *
     * @param id
     * @return
     */
    public AddressBook getById(Long id) {
        AddressBook addressBook = addressBookMapper.getById(id);
        return addressBook;
    }

    /**
     * 根据id修改地址
     *
     * @param addressBook
     */
    public void update(AddressBook addressBook) {
        addressBookMapper.update(addressBook);
    }

    /**
     * 根据id删除地址
     *
     * @param id
     */
    public void delete(Long id) {
        addressBookMapper.delete(id);
    }

    /**
     * 设置默认地址
     * @param addressBook
     */
    @Transactional
    public void setDefault(AddressBook addressBook) {
        // 将当前所有地址修改为非默认地址
        AddressBook updateAddressBook = new AddressBook();
        updateAddressBook.setIsDefault(0);
        updateAddressBook.setUserId(BaseContext.getCurrentId());
        addressBookMapper.updateIsDefault(updateAddressBook);
        //将该地址修改为默认地址
        addressBook.setIsDefault(1);
        addressBookMapper.update(addressBook);
    }


}
