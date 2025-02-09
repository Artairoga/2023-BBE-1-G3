package com.hotel.continental.model.core.service;

import com.hotel.continental.api.core.service.IRefrigeratorStockService;
import com.hotel.continental.model.core.dao.*;
import com.hotel.continental.model.core.tools.Messages;
import com.hotel.continental.model.core.tools.Validation;
import com.ontimize.jee.common.db.SQLStatementBuilder;
import com.ontimize.jee.common.dto.EntityResult;
import com.ontimize.jee.common.dto.EntityResultMapImpl;
import com.ontimize.jee.server.dao.DefaultOntimizeDaoHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import java.util.*;
import com.ontimize.jee.common.db.SQLStatementBuilder.BasicExpression;
import com.ontimize.jee.common.db.SQLStatementBuilder.BasicField;
import com.ontimize.jee.common.db.SQLStatementBuilder.BasicOperator;

@Lazy
@Service("RefrigeratorStockService")
public class RefrigeratorStockService implements IRefrigeratorStockService {
    @Autowired
    RefrigeratorStockDao refrigeratorStockDao;
    @Autowired
    RefrigeratorsDao refrigeratorsDao;
    @Autowired
    ProductsDao productDao;
    @Autowired
    DefaultOntimizeDaoHelper daoHelper;
    @Autowired
    ExtraExpensesDao extraExpensesDao;
    @Autowired
    BookingDao bookingDao;
    
    @Override
    public EntityResult refrigeratorDefaultUpdate(Map<String, Object> attrMap, Map<?, ?> keyMap) {
        //attrMap = stock / keyMap = productid
        EntityResult er = new EntityResultMapImpl();
        er.setCode(1);
        if (keyMap.get(RefrigeratorStockDao.PRODUCT_ID) == null || attrMap.get(RefrigeratorStockDao.STOCK) == null) {
            er.setMessage(Messages.NECESSARY_DATA);
            return er;
        }

        //Compruebo que el stock es un número y es positivo
        EntityResult checkNumber = Validation.checkNumber(String.valueOf(attrMap.get(RefrigeratorStockDao.STOCK)), Messages.STOCK_NOT_POSITIVE, Messages.STOCK_NOT_NUMBER);
        if(checkNumber.getCode() == EntityResult.OPERATION_WRONG) {
            return checkNumber;
        }

        //Compruebo que el producto existe
        Map<String, Object> filterProduct = new HashMap<>();
        filterProduct.put(RefrigeratorStockDao.PRODUCT_ID, keyMap.get(RefrigeratorStockDao.PRODUCT_ID));
        EntityResult product = this.daoHelper.query(this.productDao, filterProduct, List.of(RefrigeratorStockDao.PRODUCT_ID));
        if(product.calculateRecordNumber() == 0) {
            er.setMessage(Messages.PRODUCT_NOT_EXIST);
            return er;
        }

        filterProduct.put(RefrigeratorStockDao.FRIDGE_ID, -1);
        //Obtenemos si ya existe ese producto en la nevera default, si no lo añadimos
        EntityResult stock = this.daoHelper.query(this.refrigeratorStockDao, filterProduct, List.of(RefrigeratorStockDao.STOCK_ID));
        if (stock.calculateRecordNumber() == 0) {
            filterProduct.put(RefrigeratorStockDao.STOCK, attrMap.get(RefrigeratorStockDao.STOCK));
            return this.daoHelper.insert(this.refrigeratorStockDao, filterProduct);
        }

        Map<String, Object> mapStockid = new HashMap<>();
        mapStockid.put(RefrigeratorStockDao.STOCK_ID, stock.getRecordValues(0).get(RefrigeratorStockDao.STOCK_ID));

        return this.daoHelper.update(this.refrigeratorStockDao, attrMap, mapStockid);
    }

    @Override
    public EntityResult refrigeratorstockQuery(Map<String, Object> keyMap, List<String> attrList) {
        EntityResult er = new EntityResultMapImpl();
        er.setCode(1);
        if (attrList.isEmpty()) {
            er.setMessage(Messages.NECESSARY_DATA);
            return er;
        }
        EntityResult stock = this.daoHelper.query(this.refrigeratorStockDao, keyMap, attrList);
        if (stock.calculateRecordNumber() == 0) {
            er.setMessage(Messages.NOT_REGISTERS_FOUND);
            return er;
        }
        return stock;
    }

    @Override
    public EntityResult refrigeratorStockUpdate(Map<String, Object> attrMap, Map<String, Object> keyMap) {
        //attrMap: quantity -> -1, 2 -3...
        //keyMap: refrigeratorid, productid
        EntityResult er = new EntityResultMapImpl();
        er.setCode(1);
        //Probamos que se mandan lo necesario
        if (keyMap.get(RefrigeratorStockDao.FRIDGE_ID) == null || keyMap.get(RefrigeratorStockDao.PRODUCT_ID) == null) {
            er.setMessage(Messages.NECESSARY_KEY);
            return er;
        }
        if (attrMap.get(RefrigeratorStockDao.STOCK) == null) {
            er.setMessage(Messages.NECESSARY_DATA);
            return er;
        }
        //Si mandamos la nevera default la bloquea
        if ((Integer) keyMap.get(RefrigeratorStockDao.FRIDGE_ID) == -1) {
            er.setMessage(Messages.REFRIGERATOR_BLOCKED);
            return er;
        }
        //Si mandamos un codigo de una nevera o un producto que no existe falla
        Map<String, Object> fridgemap = new HashMap<>();
        fridgemap.put(RefrigeratorsDao.FRIDGE_ID, keyMap.get(RefrigeratorStockDao.FRIDGE_ID));
        EntityResult existFridge = this.daoHelper.query(this.refrigeratorsDao, fridgemap, List.of(RefrigeratorsDao.FRIDGE_ID));
        if (existFridge.calculateRecordNumber() == 0) {
            er.setMessage(Messages.REFRIGERATOR_NOT_EXIST);
            return er;
        }
        Map<String, Object> productmap = new HashMap<>();
        productmap.put(ProductsDao.PRODUCT_ID, keyMap.get(RefrigeratorStockDao.PRODUCT_ID));
        EntityResult existproduct = this.daoHelper.query(this.productDao, productmap, List.of(ProductsDao.PRODUCT_ID));
        if (existproduct.calculateRecordNumber() == 0) {
            er.setMessage(Messages.PRODUCT_NOT_EXIST);
            return er;
        }

        //Si un producto no existe ni en la nevera fisica ni en la nevera default falla por que no es necesario
        EntityResult stockid = this.daoHelper.query(this.refrigeratorStockDao, keyMap, List.of(RefrigeratorStockDao.STOCK_ID));
        Map<String, Object> keyMapDefault = new HashMap<>();
        keyMapDefault.putAll(keyMap);
        keyMapDefault.put(RefrigeratorStockDao.FRIDGE_ID, -1);
        EntityResult stockdefault = this.daoHelper.query(this.refrigeratorStockDao, keyMapDefault, List.of(RefrigeratorStockDao.STOCK));
        if (stockid.calculateRecordNumber() == 0 && stockdefault.calculateRecordNumber() == 0) {
            er.setMessage(Messages.PRODUCT_NOT_NECESSARY);
            return er;
        }
        //Si mandamos el stock 0 como no cambia nada falla
        int update = (Integer) attrMap.get(RefrigeratorStockDao.STOCK);
        EntityResult updateStock = this.daoHelper.query(this.refrigeratorStockDao, keyMap, List.of(RefrigeratorStockDao.STOCK));
        if (update == 0) {
            er.setMessage(Messages.UPDATE_STOCK_ZERO);
            return er;
        }
        //Si con esos criterios no existe ningun registro, y existe en la default, insertamos
        if (updateStock.calculateRecordNumber() == 0) {
            Map<String, Object> data = new HashMap<>();
            data.putAll(attrMap);
            data.put(RefrigeratorStockDao.FRIDGE_ID, keyMap.get(RefrigeratorStockDao.FRIDGE_ID));
            data.put(RefrigeratorStockDao.PRODUCT_ID, keyMap.get(RefrigeratorStockDao.PRODUCT_ID));
            if ((Integer) data.get(RefrigeratorStockDao.STOCK) < 0) {
                er.setMessage(Messages.NEW_STOCK_UNDER_ZERO);
                return er;
            }
            return this.daoHelper.insert(this.refrigeratorStockDao, data);
        }

        //Calculamos que no haya más en el default que en la fisica, y que no haya menos que 0
        int stock = (Integer) updateStock.getRecordValues(0).get(RefrigeratorStockDao.STOCK);
        int newStock = stock + update;
        int defaultStock = (Integer) stockdefault.getRecordValues(0).get(RefrigeratorStockDao.STOCK);
        if (newStock > defaultStock) {
            er.setMessage(Messages.NEW_STOCK_HIGHER_THAN_DEFAULT);
            return er;
        }
        if (newStock < 0) {
            er.setMessage(Messages.NEW_STOCK_UNDER_ZERO);
            return er;
        }
        //Lo añadimos a extra expense
        if ((Integer) attrMap.get(RefrigeratorStockDao.STOCK) < 0) {
            Map<String, Object> ids = new HashMap<>();
            ids.putAll(attrMap);
            ids.putAll(keyMap);
            er = addExtraExpense(ids);
            if (er.getCode()==1){
                return er;
            }
        }

        //Updateamos el stock
        attrMap.put(RefrigeratorStockDao.STOCK, newStock);
        Map<String, Object> filter = new HashMap<>();
        filter.put(RefrigeratorStockDao.STOCK_ID, stockid.getRecordValues(0).get(RefrigeratorStockDao.STOCK_ID));

        return this.daoHelper.update(this.refrigeratorStockDao, attrMap, filter);
    }

    public EntityResult addExtraExpense(Map<String, Object> map) {
        Map<String, Object> productFilter = new HashMap<>();
        productFilter.put(ProductsDao.PRODUCT_ID, map.get(RefrigeratorStockDao.PRODUCT_ID));
        List<String> productColumns = new ArrayList<>();
        productColumns.add(ProductsDao.NAME);
        productColumns.add(ProductsDao.PRICE);
        EntityResult product = this.daoHelper.query(this.productDao, productFilter, productColumns);
        Date now = new Date();

        String concept = "Fridge: " + (String) product.getRecordValues(0).get(ProductsDao.NAME) + " / " + now;
        Double price = (Double) product.getRecordValues(0).get(ProductsDao.PRICE);

        Map<String, Object> refrigeratorFilter = new HashMap<>();
        refrigeratorFilter.put(RefrigeratorsDao.FRIDGE_ID, map.get(RefrigeratorStockDao.FRIDGE_ID));
        EntityResult room = this.daoHelper.query(this.refrigeratorsDao, refrigeratorFilter, List.of(RefrigeratorsDao.ROOM_ID));
        int roomid = (Integer) room.getRecordValues(0).get(RefrigeratorsDao.ROOM_ID);
        BasicField initialDate = new BasicField(BookingDao.STARTDATE);
        BasicField endDate = new BasicField(BookingDao.ENDDATE);
        BasicExpression bexp1 = new BasicExpression(initialDate, BasicOperator.LESS_EQUAL_OP, now); //initialDate >= today
        BasicExpression bexp2 = new BasicExpression(endDate, BasicOperator.MORE_EQUAL_OP, now);//endDate >= today
        BasicExpression bexp12 = new BasicExpression(bexp1, BasicOperator.AND_OP, bexp2);//initialDate <= today AND endDate <= today
        Map<String, Object> filterDates = new HashMap<>();
        filterDates.put(BookingDao.ROOMID, roomid);
        filterDates.put(SQLStatementBuilder.ExtendedSQLConditionValuesProcessor.EXPRESSION_KEY, bexp12);
        EntityResult booking = this.daoHelper.query(this.bookingDao, filterDates, List.of(BookingDao.BOOKINGID));

        if (booking.calculateRecordNumber() == 0) {
            EntityResult er = new EntityResultMapImpl();
            er.setCode(1);
            er.setMessage(Messages.BOOKING_NOT_EXIST);
            return er;
        }

        int bookingid = (Integer) booking.getRecordValues(0).get(BookingDao.BOOKINGID);

        Map<String, Object> data = new HashMap<>();
        data.put(ExtraExpensesDao.BOOKING_ID, bookingid);
        data.put(ExtraExpensesDao.CONCEPT, concept);
        double finalprice = 0;
        for (int i = 0; i<Math.abs((Integer) map.get(RefrigeratorStockDao.STOCK));i++) {
            finalprice = finalprice + price;
        }
        data.put(ExtraExpensesDao.PRICE, finalprice);
        return this.daoHelper.insert(this.extraExpensesDao, data);
    }
}