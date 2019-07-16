/*
 * Copyright (c) 2009-2012 LinogistiX GmbH
 * 
 *  www.linogistix.com
 *  
 *  Project myWMS-LOS
 */
package de.linogistix.los.inventory.businessservice;

import java.util.Date;

import javax.ejb.Local;

import org.mywms.facade.FacadeException;
import org.mywms.model.Client;

import de.linogistix.los.inventory.exception.InventoryException;
import de.linogistix.los.inventory.model.LOSGoodsOutRequest;
import de.linogistix.los.inventory.model.LOSGoodsOutRequestPosition;
import de.wms2.mywms.delivery.DeliveryOrder;
import de.wms2.mywms.inventory.UnitLoad;
import de.wms2.mywms.location.StorageLocation;

/**
 * Generation of shipping orders
 * @author krane
 *
 */
@Local
public interface LOSGoodsOutGenerator {

	public LOSGoodsOutRequest createOrder( DeliveryOrder customerOrder ) throws FacadeException;
	
	public LOSGoodsOutRequest createOrder( Client client, StorageLocation outLocation, String shipmentNumber, Date shippingDate, String courier, String additionalInfo) throws FacadeException;

	public LOSGoodsOutRequestPosition addPosition(LOSGoodsOutRequest out, UnitLoad unitLoad) throws FacadeException;

	public void removePosition(LOSGoodsOutRequest out, UnitLoad unitLoad) throws InventoryException;

}
