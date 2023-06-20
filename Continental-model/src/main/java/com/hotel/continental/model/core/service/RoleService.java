package com.hotel.continental.model.core.service;

import com.hotel.continental.api.core.service.IRoleService;
import com.hotel.continental.model.core.dao.HotelDao;
import com.hotel.continental.model.core.dao.RoleDao;
import com.hotel.continental.model.core.dao.RoomDao;
import com.hotel.continental.model.core.tools.ErrorMessages;
import com.ontimize.jee.common.dto.EntityResult;
import com.ontimize.jee.common.dto.EntityResultMapImpl;
import com.ontimize.jee.common.security.PermissionsProviderSecured;
import com.ontimize.jee.server.dao.DefaultOntimizeDaoHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

@Lazy
@Service("RoleService")
public class RoleService implements IRoleService {
    @Autowired
    private RoleDao roleDao;

    @Autowired
    private DefaultOntimizeDaoHelper daoHelper;
    @Override
    @Secured({ PermissionsProviderSecured.SECURED })
    public EntityResult roleQuery(Map<?, ?> keyMap, List<?> attrList) {
        EntityResult role = this.daoHelper.query(this.roleDao, keyMap, attrList);
        if(role == null || role.calculateRecordNumber() == 0){
            EntityResult er;
            er = new EntityResultMapImpl();
            er.setCode(EntityResult.OPERATION_WRONG);
            er.setMessage(ErrorMessages.ROLE_DOESNT_EXIST);
            return er;
        }
        return role;
    }

    @Override
    public EntityResult roleInsert(Map<String, Object> attrMap) {
        //Comnprobamos que nos mandan los atributos necesarios(rolename)
        if(!attrMap.containsKey(RoleDao.ROLENAME)){
            EntityResult er;
            er = new EntityResultMapImpl();
            er.setCode(EntityResult.OPERATION_WRONG);
            er.setMessage(ErrorMessages.NECESSARY_DATA);
            return er;
        }
        //Comprobamos que no esta vacio y que no es nulo
        if(attrMap.get(RoleDao.ROLENAME) == null || attrMap.get(RoleDao.ROLENAME).toString().isEmpty()){
            EntityResult er;
            er = new EntityResultMapImpl();
            er.setCode(EntityResult.OPERATION_WRONG);
            er.setMessage(ErrorMessages.NECESSARY_DATA);
            return er;
        }
        attrMap.put(RoleDao.ROLENAME, attrMap.remove(RoleDao.ROLENAME).toString().toLowerCase());
        attrMap.put(RoleDao.XMLCLIENTPERMISSION,"<?xml version=\"1.0\" encoding=\"UTF-8\"?><security></security>");
        //Primero comprobamos que el rol que nos pasan no existe
        EntityResult role = this.daoHelper.query(this.roleDao, attrMap, Arrays.asList(RoleDao.ROLENAME));
        if(role.calculateRecordNumber() > 0){
            EntityResult er;
            er = new EntityResultMapImpl();
            er.setCode(EntityResult.OPERATION_WRONG);
            er.setMessage(ErrorMessages.ROLE_ALREADY_EXISTS);
            return er;
        }
        //Insertamos el rol
        EntityResult er = this.daoHelper.insert(this.roleDao, attrMap);
        return er;
    }

    @Override
    public EntityResult roleDelete(Map<?, ?> keyMap) {
        if (!keyMap.containsKey(RoleDao.ID_ROLENAME)) {
            EntityResult er;
            er = new EntityResultMapImpl();
            er.setCode(EntityResult.OPERATION_WRONG);
            er.setMessage(ErrorMessages.NECESSARY_KEY);
            return er;
        }
        if (keyMap.get(RoleDao.ID_ROLENAME).equals("0")) {
            EntityResult er = new EntityResultMapImpl();
            er.setCode(EntityResult.OPERATION_WRONG);
            er.setMessage(ErrorMessages.ADMIN_ROLE_NOT_EDITABLE);
            return er;
        }
        //si el rol no existe lanzar un error
        EntityResult role = roleQuery(keyMap, Arrays.asList(RoleDao.ID_ROLENAME));
        if (role.getCode() == EntityResult.OPERATION_WRONG) {
            return role;
        }
        EntityResult er = this.daoHelper.delete(this.roleDao, keyMap);
        er.setMessage("El role con codigo " + keyMap.get(RoleDao.ID_ROLENAME) + " ha sido borrado correctamente");
        return er;
    }
}
