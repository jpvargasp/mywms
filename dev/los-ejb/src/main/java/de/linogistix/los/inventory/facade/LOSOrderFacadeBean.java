/*
 * Copyright (c) 2011-2012 LinogistiX GmbH
 * 
 *  www.linogistix.com
 *  
 *  Project myWMS-LOS
 */
package de.linogistix.los.inventory.facade;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.enterprise.event.Event;
import javax.enterprise.event.ObserverException;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.log4j.Logger;
import org.mywms.facade.FacadeException;
import org.mywms.model.Client;
import org.mywms.service.ClientService;

import de.linogistix.los.inventory.businessservice.LOSOrderBusiness;
import de.linogistix.los.inventory.businessservice.LOSOrderGenerator;
import de.linogistix.los.inventory.customization.ManageOrderService;
import de.linogistix.los.inventory.exception.InventoryException;
import de.linogistix.los.inventory.exception.InventoryExceptionKey;
import de.linogistix.los.inventory.service.ItemDataService;
import de.linogistix.los.inventory.service.LOSCustomerOrderService;
import de.linogistix.los.inventory.service.LOSGoodsOutRequestPositionService;
import de.linogistix.los.inventory.service.LOSPickingPositionService;
import de.linogistix.los.inventory.service.LOSStorageRequestService;
import de.linogistix.los.inventory.service.QueryLotService;
import de.linogistix.los.location.exception.LOSLocationException;
import de.linogistix.los.location.exception.LOSLocationExceptionKey;
import de.linogistix.los.model.State;
import de.linogistix.los.query.BODTO;
import de.linogistix.los.util.StringTools;
import de.linogistix.los.util.businessservice.ContextService;
import de.wms2.mywms.address.Address;
import de.wms2.mywms.address.AddressEntityService;
import de.wms2.mywms.delivery.DeliveryOrder;
import de.wms2.mywms.delivery.DeliveryOrderLine;
import de.wms2.mywms.delivery.DeliveryOrderStateChangeEvent;
import de.wms2.mywms.delivery.DeliverynoteReportGenerator;
import de.wms2.mywms.document.Document;
import de.wms2.mywms.exception.BusinessException;
import de.wms2.mywms.inventory.Lot;
import de.wms2.mywms.inventory.StockUnit;
import de.wms2.mywms.inventory.UnitLoad;
import de.wms2.mywms.inventory.UnitLoadEntityService;
import de.wms2.mywms.inventory.UnitLoadReportGenerator;
import de.wms2.mywms.location.StorageLocation;
import de.wms2.mywms.location.StorageLocationEntityService;
import de.wms2.mywms.picking.Packet;
import de.wms2.mywms.picking.PacketEntityService;
import de.wms2.mywms.picking.PickingOrder;
import de.wms2.mywms.picking.PickingOrderEntityService;
import de.wms2.mywms.picking.PickingOrderGenerator;
import de.wms2.mywms.picking.PickingOrderLine;
import de.wms2.mywms.picking.PickingOrderLineGenerator;
import de.wms2.mywms.picking.PickingOrderPrioChangeEvent;
import de.wms2.mywms.product.ItemData;
import de.wms2.mywms.shipping.ShippingOrder;
import de.wms2.mywms.shipping.ShippingOrderEntityService;
import de.wms2.mywms.shipping.ShippingOrderLine;
import de.wms2.mywms.shipping.ShippingOrderLineEntityService;
import de.wms2.mywms.strategy.OrderState;
import de.wms2.mywms.strategy.OrderStrategy;
import de.wms2.mywms.strategy.OrderStrategyEntityService;
import de.wms2.mywms.transport.TransportBusiness;
import de.wms2.mywms.transport.TransportOrder;
import de.wms2.mywms.transport.TransportOrderEntityService;


// TODO i18n

/**
 * @author krane
 *
 */
@Stateless
public class LOSOrderFacadeBean implements LOSOrderFacade {
	private Logger log = Logger.getLogger(LOSOrderFacadeBean.class);
	
	@EJB
	private ClientService clientService;
	@EJB
	private LOSCustomerOrderService orderService;
	@EJB
	private LOSPickingPositionService pickingPositionService;
	@EJB
	private LOSOrderBusiness orderBusiness;
	@EJB
	private LOSOrderGenerator orderGenerator;
	@EJB
	private ItemDataService itemService;
	@EJB
	private QueryLotService lotService;
	@Inject
	private PickingOrderLineGenerator pickingPosGenerator;
	@Inject
	private PickingOrderGenerator pickingOrderGenerator;
	@EJB
	private ContextService contextService;
	@EJB
	private OrderStrategyEntityService orderStratService;
	@EJB
	private ManageOrderService manageOrderService;
	@EJB
	private LOSGoodsOutRequestPositionService outPosService;
	@EJB
	private LOSStorageRequestService storageService;
	@Inject
	private UnitLoadReportGenerator unitLoadReport;
    @PersistenceContext(unitName = "myWMS")
    private  EntityManager manager;

    @Inject
    private PickingOrderEntityService pickingOrderEntityService;
	@Inject
	private PacketEntityService pickingUnitLoadService;
	@Inject
	private StorageLocationEntityService locationService;
	@Inject
	private UnitLoadEntityService unitLoadService;
	@Inject
	private DeliverynoteReportGenerator deliverynoteGenerator;
	@Inject
	private TransportOrderEntityService transportOrderService;
	@Inject
	private TransportBusiness transportBusiness;
	@Inject
	private ShippingOrderLineEntityService shippingOrderLineService;
	@Inject
	private ShippingOrderEntityService shippingOrderService;
	@Inject
	private AddressEntityService addressEntityService;
	@Inject
	private Event<DeliveryOrderStateChangeEvent> deliveryOrderStateChangeEvent;
	@Inject
	private Event<PickingOrderPrioChangeEvent> pickingOrderPrioChangeEvent;

	public DeliveryOrder order(String clientNumber, String externalNumber, OrderPositionTO[] positions,
			String documentUrl, String labelUrl, String destinationName, String orderStrategyName, Date deliveryDate,
			int prio, boolean startPicking, boolean completeOnly, String comment) throws FacadeException {
		return order(clientNumber, externalNumber, positions, documentUrl, labelUrl, destinationName, orderStrategyName,
				deliveryDate, prio, null, startPicking, completeOnly, comment);
	}

	public DeliveryOrder order(
			String clientNumber,
			String externalNumber,
			OrderPositionTO[] positions,
			String documentUrl,
			String labelUrl,
			String destinationName, 
			String orderStrategyName,
			Date deliveryDate, 
			int prio,
			Address address,
			boolean startPicking, boolean completeOnly,
			String comment) throws FacadeException {
		String logStr = "order ";
		log.debug(logStr);
		
		DeliveryOrder order;

		Client client = null;
		if( StringTools.isEmpty(clientNumber) ) {
			client = contextService.getCallersClient();
		}
		else {
			client = clientService.getByNumber(clientNumber);
		}
		if( client == null ){
			String msg = "Client does not exist. number="+clientNumber;
			log.error(logStr+msg);
			throw new InventoryException(InventoryExceptionKey.CUSTOM_TEXT, msg);
		}

		if(deliveryDate == null){
			deliveryDate = new Date(System.currentTimeMillis() + (24 * 3600 * 1000));
		}

		OrderStrategy strat = null;
		if( StringTools.isEmpty(orderStrategyName) ) {
			strat = orderStratService.getDefault(client);
		}
		else {
			strat = orderStratService.read(orderStrategyName);
		}
		if( strat == null ){
			String msg = "OrderStrategy does not exist. name="+orderStrategyName;
			log.error(logStr+msg);
			throw new InventoryException(InventoryExceptionKey.CUSTOM_TEXT, msg);
		}
		
		StorageLocation destination = null;
		if( destinationName != null && destinationName.length()>0 ) {
			destination = locationService.read(destinationName);
			if( destination == null ){
				String msg = "Location does not exist. name="+destination;
				log.error(logStr+msg);
				throw new InventoryException(InventoryExceptionKey.CUSTOM_TEXT, msg);
			}
		}
		
		order = orderGenerator.createDeliveryOrder(client, strat);
		order.setPrio(prio);
		order.setAdditionalContent(comment);
		order.setExternalNumber(externalNumber);
		order.setDocumentUrl(documentUrl);
		order.setLabelUrl(labelUrl);
		order.setDestination(destination);
		order.setDeliveryDate(deliveryDate);
		order.setAddress(address);

		for( OrderPositionTO posTO : positions ) {
			ItemData item = itemService.getByItemNumber(client, posTO.articleRef);
			if( item == null ) {
				String msg = "Item data does not exist. number="+posTO.articleRef;
				log.error(logStr+msg);
				throw new InventoryException(InventoryExceptionKey.CUSTOM_TEXT, msg);
			}
			Lot lot = null;
			if( posTO.batchRef != null && posTO.batchRef.length()>0 ) {
				lot = lotService.getByNameAndItemData(posTO.batchRef, item);
				if( lot == null ) {
					String msg = "Lot data does not exist. name="+posTO.batchRef+", item="+posTO.articleRef;
					log.error(logStr+msg);
					throw new InventoryException(InventoryExceptionKey.CUSTOM_TEXT, msg);
				}
			}
			BigDecimal amount = posTO.amount;
			if( BigDecimal.ZERO.compareTo(amount)>=0 ) {
				String msg = "Amount must not be <= 0. amount="+posTO.amount+", item="+posTO.articleRef;
				log.error(logStr+msg);
				throw new InventoryException(InventoryExceptionKey.CUSTOM_TEXT, msg);
			}
			
			orderGenerator.addDeliveryOrderLine(order, item, lot, null, amount);
		}

		if( startPicking ) {

			List<PickingOrderLine> pickList;
			pickList = pickingPosGenerator.generatePicks(order, completeOnly);
			if (pickList != null && pickList.size() > 0) {
				Collection<PickingOrder> pickingOrders = pickingOrderGenerator.generatePickingOrders(pickList);
				for (PickingOrder pickingOrder : pickingOrders) {
					pickingOrder.setDestination(destination);
					pickingOrder.setPrio(prio);
					orderBusiness.releasePickingOrder(pickingOrder);
				}
			}
		}
		
		return order;
	}


	public DeliveryOrder finishOrder(Long orderId) throws FacadeException {
		String logStr = "finishOrder ";
		
		DeliveryOrder order = manager.find(DeliveryOrder.class, orderId);
		if( order == null ) {
			String msg = "Customer order does not exist. id="+orderId;
			log.error(logStr+msg);
			throw new InventoryException(InventoryExceptionKey.CUSTOM_TEXT, msg);
		}
		log.debug(logStr+"order number="+order.getOrderNumber());

		List<PickingOrderLine> pickList = pickingPositionService.getByDeliveryOrder(order);
		Set<PickingOrder> pickingOrderSet = new HashSet<PickingOrder>();
		for( PickingOrderLine pick : pickList ) {
			PickingOrder po = pick.getPickingOrder();
			if( po != null ) {
				pickingOrderSet.add(po);
			}
			orderBusiness.cancelPick(pick);
		}
		for( PickingOrder po : pickingOrderSet ) {
			orderBusiness.recalculatePickingOrderState(po);
		}
		orderBusiness.finishDeliveryOrder(order);
		
		return order;
	}
	
	public void removeOrder(Long orderId) throws FacadeException {
		String logStr = "removeOrder ";
		
		DeliveryOrder order = manager.find(DeliveryOrder.class, orderId);
		if( order == null ) {
			String msg = "Customer order does not exist. id="+orderId;
			log.error(logStr+msg);
			throw new InventoryException(InventoryExceptionKey.CUSTOM_TEXT, msg);
		}
		log.debug(logStr+"order number="+order.getOrderNumber());
		
		List<PickingOrderLine> pickList = pickingPositionService.getByDeliveryOrder(order);
		Set<Long> pickingOrderSet1 = new HashSet<Long>();
		
		// 1. Remove all picks
		for( PickingOrderLine pick : pickList ) {
			PickingOrder po = pick.getPickingOrder();
			if( po != null ) {
				pickingOrderSet1.add(po.getId());
			}
			if( pick.getState()<State.PICKED ) {
				orderBusiness.cancelPick(pick);
			}
			manager.remove(pick);
		}
		
		// 2. remove all customer order positions
		for( DeliveryOrderLine pos : order.getLines() ) {
			manager.remove(pos);
		}

		manager.flush();
		
		// 3. Find all LOSPickingOrder without position 
		Set<PickingOrder> pickingOrderSetRemovable = new HashSet<PickingOrder>();
		Set<Packet> pickingUnitLoadSetRemovable = new HashSet<Packet>();
		Set<ShippingOrder> goodsOutRequestSetRemovable = new HashSet<>();

		for( Long poId : pickingOrderSet1 ) {
			PickingOrder po = manager.find(PickingOrder.class, poId);
			if( po != null && po.getLines().size()==0 ) {
				pickingOrderSetRemovable.add(po);
				pickingUnitLoadSetRemovable.addAll( pickingUnitLoadService.readByPickingOrder(po) );
			}
		}


		// 4. Remove all LOSPickingUnitLoad and LOSGoodsOutRequestPosition with removable LOSPickingOrder
		for( Packet pul : pickingUnitLoadSetRemovable ) {
			UnitLoad ul = pul.getUnitLoad();
			

			List<TransportOrder> storageList = transportOrderService.readList(ul, null, null, null);
			for( TransportOrder storageReq : storageList ) {
				transportBusiness.removeUnitLoadReference(storageReq);
			}
			
			List<ShippingOrderLine> outPosList = shippingOrderLineService.readListByPacket(pul);
			for( ShippingOrderLine outPos : outPosList ) {
				goodsOutRequestSetRemovable.add(outPos.getShippingOrder());
				manager.remove(outPos);
			}
			
			for( StockUnit su : ul.getStockUnitList() ) {
				manager.remove(su);
			}
			manager.remove(pul);
			manager.remove(ul);
		}

		// 5. Remove empty picking orders
		for( PickingOrder po : pickingOrderSetRemovable ) {
			manager.remove(po);
		}
		
		manager.flush();
		
		// 6. Remove empty shipping orders
		for( ShippingOrder outReq : goodsOutRequestSetRemovable ) {
			if( outReq != null && outReq.getLines().size()<=0 ) {
				manager.remove(outReq);
			}
		}
		
		// 7. Remove directly addressed shipping orders
		List<ShippingOrder> outList = shippingOrderService.readByDeliveryOrder(order);
		for( ShippingOrder outReq : outList ) {
			if( outReq == null ) {
				continue;
			}
			for( ShippingOrderLine outPos : outReq.getLines() ) {
				manager.remove(outPos);
			}
			manager.remove(outReq);
		}

		
		manager.flush();

		// 8. Remove address
		Address address = order.getAddress();
		if (address != null) {
			order.setAddress(null);
			addressEntityService.removeIfUnused(address);
		}

		// 9. Remove order
		manager.remove(order);
	}

	
	public List<String> getGoodsOutLocations() throws FacadeException {
		String logStr = "getGoodsOutLocations ";
		log.debug(logStr);
		
		
		List<StorageLocation> slList;
		slList = locationService.getForGoodsOut(null);

		if (slList.size() == 0) {
			throw new LOSLocationException(
					// LOSLocationExceptionKey.NO_GOODS_IN_LOCATION, new
					// Object[0]);
					LOSLocationExceptionKey.NO_GOODS_OUT_LOCATION,
					new Object[0]);
		}
		
		List<String> ret = new ArrayList<String>();

		for (StorageLocation sl : slList) {
			ret.add(sl.getName());
		}
		return ret;
	}
	
	public List<BODTO<StorageLocation>> getGoodsOutLocationsBO() throws FacadeException {
		String logStr = "getGoodsOutLocationsBO ";
		log.debug(logStr);
		
		
		List<StorageLocation> slList;
		slList = locationService.getForGoodsOut(null);

		if (slList.size() == 0) {
			throw new LOSLocationException(
					// LOSLocationExceptionKey.NO_GOODS_IN_LOCATION, new
					// Object[0]);
					LOSLocationExceptionKey.NO_GOODS_OUT_LOCATION,
					new Object[0]);
		}
		
		List<BODTO<StorageLocation>> ret = new ArrayList<BODTO<StorageLocation>>();

		for (StorageLocation sl : slList) {
			ret.add( new BODTO<StorageLocation>(sl) );
		}
		return ret;
	}

	public void changeOrderPrio( Long orderId, int prio ) throws FacadeException {
		String logStr = "changeOrderPrio ";
		DeliveryOrder order = manager.find(DeliveryOrder.class, orderId);
		if( order == null ) {
			String msg = "Customer order does not exist. id="+orderId;
			log.error(logStr+msg);
			throw new InventoryException(InventoryExceptionKey.CUSTOM_TEXT, msg);
		}
		log.debug(logStr+"order number="+order.getOrderNumber());

		order.setPrio(prio);
		
		List<PickingOrder> poList = pickingOrderEntityService.readByDeliveryOrder(order);
		for( PickingOrder po : poList ) {
			int prioOld = po.getPrio();
			if( prio != prioOld ) {
				po.setPrio(prio);
				firePickingOrderPrioChangeEvent(po, prioOld);
			}
		}
	}
	
	
	public Document generateReceipt( Long orderId ) throws FacadeException {
		String logStr = "generateReceipt ";
		DeliveryOrder order = manager.find(DeliveryOrder.class, orderId);
		if( order == null ) {
			String msg = "Customer order does not exist. id="+orderId;
			log.error(logStr+msg);
			throw new InventoryException(InventoryExceptionKey.CUSTOM_TEXT, msg);
		}
		log.debug(logStr+"order number="+order.getOrderNumber());

		return  deliverynoteGenerator.generateReport(order);
	}
	
	public Document generateUnitLoadLabel( String label ) throws FacadeException {
		String logStr = "generateUnitLoadLabel ";
		log.debug(logStr+"label="+label);


		UnitLoad unitLoad = unitLoadService.readByLabel(label);
		if( unitLoad == null ) {
			String msg = "Unit load does not exist. label="+label;
			log.error(logStr+msg);
			throw new InventoryException(InventoryExceptionKey.CUSTOM_TEXT, msg);
		}

		Document doc = unitLoadReport.generateReport(unitLoad);
		
		return doc;
	}
	
	public void processOrderPickedFinish(List<BODTO<DeliveryOrder>> orders) throws FacadeException {
		String logStr = "processOrderPickedFinish ";
		if (orders == null) {
			return;
		}
	
		for (BODTO<DeliveryOrder> order : orders) {
			DeliveryOrder deliveryOrder = manager.find(DeliveryOrder.class, order.getId());
			if( deliveryOrder==null ) {
				continue;
			}
			log.debug(logStr+"Order="+deliveryOrder.getOrderNumber());
				
			List<PickingOrder> pickOrderList = pickingOrderEntityService.readByDeliveryOrder(deliveryOrder);
			for( PickingOrder pickingOrder : pickOrderList ) {
				if( pickingOrder.getState() >= State.FINISHED  ) {
					continue;
				}
				orderBusiness.finishPickingOrder(pickingOrder);
			}
				
			for (DeliveryOrderLine pos : deliveryOrder.getLines()) {
				pos = manager.find(DeliveryOrderLine.class, pos.getId());
				log.debug(logStr+"Check pos="+pos.getLineNumber()+", state="+pos.getState());
			
				if( pos.getState() == State.PENDING ) {
					log.info("processOrderPicked: force closing pending position. pos=" + pos.getLineNumber());
					pos.setState(State.PICKED);
				}
			}

			int stateOld = deliveryOrder.getState();
			OrderStrategy strat = deliveryOrder.getOrderStrategy();
			if( strat != null && strat.isCreatePackingOrder() ) {
				deliveryOrder.setState(OrderState.PACKING);
			}
			else if( strat != null && strat.isCreateShippingOrder() ) {
				deliveryOrder.setState(OrderState.SHIPPING);
			}
			else {
				deliveryOrder.setState(State.FINISHED);
			}
			if( deliveryOrder.getState() != stateOld ) {
				fireDeliveryOrderStateChangeEvent(deliveryOrder, stateOld);
			}
		}
	}

	private void fireDeliveryOrderStateChangeEvent(DeliveryOrder entity, int oldState) throws BusinessException {
		try {
			log.debug("Fire DeliveryOrderStateChangeEvent. entity=" + entity + ", state=" + entity.getState()
					+ ", oldState=" + oldState);
			deliveryOrderStateChangeEvent.fire(new DeliveryOrderStateChangeEvent(entity, oldState, entity.getState()));
		} catch (ObserverException ex) {
			Throwable cause = ex.getCause();
			if (cause != null && cause instanceof BusinessException) {
				throw (BusinessException) cause;
			}
			throw ex;
		}
	}

	private void firePickingOrderPrioChangeEvent(PickingOrder entity, int oldPrio) throws BusinessException {
		try {
			log.debug("Fire PickingOrderStateChangeEvent. entity=" + entity + ", prio=" + entity.getPrio()
					+ ", oldPrio=" + oldPrio);
			pickingOrderPrioChangeEvent.fire(new PickingOrderPrioChangeEvent(entity, oldPrio, entity.getPrio()));
		} catch (ObserverException ex) {
			Throwable cause = ex.getCause();
			if (cause != null && cause instanceof BusinessException) {
				throw (BusinessException) cause;
			}
			throw ex;
		}
	}
}
