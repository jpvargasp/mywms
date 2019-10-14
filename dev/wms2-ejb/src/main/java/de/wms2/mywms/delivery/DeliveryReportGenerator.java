/* 
Copyright 2019 Matthias Krane
info@krane.engineer

This file is part of the Warehouse Management System mywms

mywms is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as
published by the Free Software Foundation, either version 3 of the
License, or (at your option) any later version.
 
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
*/
package de.wms2.mywms.delivery;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.mywms.model.Client;

import de.wms2.mywms.address.Address;
import de.wms2.mywms.document.Document;
import de.wms2.mywms.document.DocumentType;
import de.wms2.mywms.exception.BusinessException;
import de.wms2.mywms.inventory.Lot;
import de.wms2.mywms.inventory.StockUnit;
import de.wms2.mywms.inventory.StockUnitEntityService;
import de.wms2.mywms.inventory.UnitLoad;
import de.wms2.mywms.picking.Packet;
import de.wms2.mywms.picking.PacketEntityService;
import de.wms2.mywms.picking.PickingOrder;
import de.wms2.mywms.picking.PickingOrderLine;
import de.wms2.mywms.product.ItemData;
import de.wms2.mywms.report.ReportBusiness;
import de.wms2.mywms.shipping.ShippingOrder;
import de.wms2.mywms.shipping.ShippingOrderLine;
import de.wms2.mywms.util.Wms2BundleResolver;

public class DeliveryReportGenerator {
	private final Logger logger = Logger.getLogger(this.getClass().getName());

	@Inject
	private PacketEntityService packetService;
	@Inject
	private ReportBusiness reportBusiness;
	@Inject
	private StockUnitEntityService stockUnitService;

	private static final String ITEM_TYPE_PACKET = "1-PACKET";
	private static final String ITEM_TYPE_ITEMDATA = "2-ITEMDATA";
	private static final String ITEM_TYPE_LOT = "3-LOT";
	private static final String ITEM_TYPE_BESTBEFORE = "4-BESTBEFORE";
	private static final String ITEM_TYPE_LOT_BESTBEFORE = "5-LOT_BESTBEFORE";
	private static final String ITEM_TYPE_SERIAL = "6-SERIAL";
	private static final String ITEM_TYPE_LINE = "7-LINE";

	public Document generatePackList(Packet packet) throws BusinessException {
		String logStr = "generatePackList ";
		logger.log(Level.INFO, logStr + "packet=" + packet);

		UnitLoad unitLoad = packet.getUnitLoad();
		Client client = unitLoad.getClient();

		DeliveryOrder deliveryOrder = packet.getDeliveryOrder();
		PickingOrder pickingOrder = packet.getPickingOrder();
		Address address = null;
		if (deliveryOrder != null) {
			address = deliveryOrder.getAddress();
		}
		if ((address == null || address.isEmpty()) && pickingOrder != null) {
			address = pickingOrder.getAddress();
		}
		if (address == null) {
			address = new Address();
		}
		String label = unitLoad.getLabelId();
		String suffix = "-" + unitLoad.getId();
		label = StringUtils.substringBefore(label, suffix);

		Map<String, DeliveryReportDto> reportItemMap = new HashMap<>();
		registerItems(reportItemMap, unitLoad, packet, true);
		List<DeliveryReportDto> reportItems = new ArrayList<>();
		reportItems.addAll(reportItemMap.values());
		Collections.sort(reportItems, new UnitLoadReportDtoComparator());

		Map<String, Object> parameters = new HashMap<>();
		parameters.put("printDate", new Date());
		parameters.put("packet", packet);
		parameters.put("unitLoad", unitLoad);
		parameters.put("unitLoadType", unitLoad.getUnitLoadType());
		parameters.put("pickingOrder", pickingOrder);
		parameters.put("deliveryOrder", deliveryOrder);
		parameters.put("address", address);
		parameters.put("label", label);

		byte[] data = reportBusiness.createPdfDocument(client, "Packlist", Wms2BundleResolver.class, reportItems,
				parameters);

		Document doc = new Document();
		doc.setData(data);
		doc.setDocumentType(DocumentType.PDF);
		doc.setName("Packlist");

		return doc;
	}

	public Document generatePackList(UnitLoad unitLoad) throws BusinessException {
		String logStr = "generatePackList ";
		logger.log(Level.INFO, logStr + "unitLoad=" + unitLoad);

		Client client = unitLoad.getClient();

		DeliveryOrder deliveryOrder = null;
		PickingOrder pickingOrder = null;
		Packet packet = packetService.readFirstByUnitLoad(unitLoad);
		if (packet != null) {
			deliveryOrder = packet.getDeliveryOrder();
			pickingOrder = packet.getPickingOrder();
		}
		Address address = null;
		if (deliveryOrder != null) {
			address = deliveryOrder.getAddress();
		}
		if ((address == null || address.isEmpty()) && pickingOrder != null) {
			address = pickingOrder.getAddress();
		}
		if (address == null) {
			address = new Address();
		}
		String label = unitLoad.getLabelId();
		String suffix = "-" + unitLoad.getId();
		label = StringUtils.substringBefore(label, suffix);

		Map<String, DeliveryReportDto> reportItemMap = new HashMap<>();
		registerItems(reportItemMap, unitLoad, packet, true);
		List<DeliveryReportDto> reportItems = new ArrayList<>();
		reportItems.addAll(reportItemMap.values());
		Collections.sort(reportItems, new UnitLoadReportDtoComparator());

		Map<String, Object> parameters = new HashMap<>();
		parameters.put("printDate", new Date());
		parameters.put("unitLoad", unitLoad);
		parameters.put("unitLoadType", unitLoad.getUnitLoadType());
		parameters.put("pickingOrder", pickingOrder);
		parameters.put("deliveryOrder", deliveryOrder);
		parameters.put("address", address);
		parameters.put("label", label);

		byte[] data = reportBusiness.createPdfDocument(client, "Packlist", Wms2BundleResolver.class, reportItems,
				parameters);

		Document doc = new Document();
		doc.setData(data);
		doc.setDocumentType(DocumentType.PDF);
		doc.setName("Packlist");

		return doc;
	}

	public Document generatePacketList(PickingOrder pickingOrder) throws BusinessException {
		String logStr = "generatePacketList ";
		logger.log(Level.INFO, logStr + "pickingOrder=" + pickingOrder);

		Client client = pickingOrder.getClient();

		Map<String, DeliveryReportDto> reportItemMap = new HashMap<>();
		for (Packet packet : pickingOrder.getPackets()) {
			registerItems(reportItemMap, packet.getUnitLoad(), packet, true);
		}
		List<DeliveryReportDto> reportItems = new ArrayList<>();
		reportItems.addAll(reportItemMap.values());
		Collections.sort(reportItems, new UnitLoadReportDtoComparator());

		Address address = pickingOrder.getAddress();
		if (address == null || address.isEmpty()) {
			DeliveryOrder deliveryOrder = pickingOrder.getDeliveryOrder();
			if (deliveryOrder != null) {
				address = deliveryOrder.getAddress();
			}
		}
		if (address == null) {
			address = new Address();
		}

		Map<String, Object> parameters = new HashMap<>();
		parameters.put("printDate", new Date());
		parameters.put("pickingOrder", pickingOrder);
		parameters.put("address", address);

		byte[] data = reportBusiness.createPdfDocument(client, "PickingPacketlist", Wms2BundleResolver.class,
				reportItems, parameters);

		Document doc = new Document();
		doc.setData(data);
		doc.setDocumentType(DocumentType.PDF);
		doc.setName("Packetlist");

		return doc;
	}

	public Document generatePacketList(ShippingOrder shippingOrder) throws BusinessException {
		String logStr = "generatePacketList ";
		logger.log(Level.INFO, logStr + "shippingOrder=" + shippingOrder);

		Client client = shippingOrder.getClient();

		Address address = shippingOrder.getAddress();
		if (address == null || address.isEmpty()) {
			DeliveryOrder deliveryOrder = shippingOrder.getDeliveryOrder();
			if (deliveryOrder != null) {
				address = deliveryOrder.getAddress();
			}
		}
		if (address == null) {
			address = new Address();
		}

		Map<String, DeliveryReportDto> reportItemMap = new HashMap<>();
		for (ShippingOrderLine shippingOrderLine : shippingOrder.getLines()) {
			Packet packet = shippingOrderLine.getPacket();
			if (packet == null) {
				continue;
			}
			UnitLoad unitLoad = packet.getUnitLoad();
			registerItems(reportItemMap, unitLoad, packet, true);
		}
		List<DeliveryReportDto> reportItems = new ArrayList<>();
		reportItems.addAll(reportItemMap.values());
		Collections.sort(reportItems, new UnitLoadReportDtoComparator());

		Map<String, Object> parameters = new HashMap<>();
		parameters.put("printDate", new Date());
		parameters.put("shippingOrder", shippingOrder);
		parameters.put("address", address);

		byte[] data = reportBusiness.createPdfDocument(client, "ShippingPacketlist", Wms2BundleResolver.class,
				reportItems, parameters);

		Document doc = new Document();
		doc.setData(data);
		doc.setDocumentType(DocumentType.PDF);
		doc.setName("Packetlist");

		return doc;
	}

	public Document generateDeliverynote(DeliveryOrder order) throws BusinessException {
		String logStr = "generateDeliverynote ";
		logger.log(Level.INFO, logStr + "order=" + order);

		Client client = order.getClient();

		Address address = order.getAddress();
		if (address == null) {
			address = new Address();
		}

		Map<ItemData, BigDecimal> orderAmountMap = new HashMap<>();
		Map<ItemData, BigDecimal> unitLoadAmountMap = new HashMap<>();
		Map<String, DeliveryReportDto> reportItemMap = new HashMap<>();

		for (DeliveryOrderLine orderLine : order.getLines()) {
			registerAmount(orderAmountMap, orderLine.getItemData(), orderLine.getPickedAmount());
		}

		List<Packet> packets = packetService.readByDeliveryOrder(order);
		for (Packet packet : packets) {
			registerItems(reportItemMap, packet.getUnitLoad(), packet, false);
		}

		for (DeliveryReportDto item : reportItemMap.values()) {
			if (item.getType().equals(ITEM_TYPE_ITEMDATA)) {
				registerAmount(unitLoadAmountMap, item.getItemData(), item.getAmount());
			}
		}

		// Not all picking packets can be resolved. Compare the sum of the amounts with
		// the sum of the picked amounts of the delivery order lines and add the
		// differences.
		for (ItemData itemData : orderAmountMap.keySet()) {
			BigDecimal orderAmount = orderAmountMap.get(itemData);
			if (orderAmount == null) {
				orderAmount = BigDecimal.ZERO;
			}
			BigDecimal unitLoadAmount = unitLoadAmountMap.get(itemData);
			if (unitLoadAmount == null) {
				unitLoadAmount = BigDecimal.ZERO;
			}

			if (orderAmount.compareTo(unitLoadAmount) > 0) {
				// More picked amount in order line than in packet
				// Add difference to sheet
				BigDecimal diffAmount = orderAmount.subtract(unitLoadAmount);
				String key = "-" + itemData.getId();
				registerItem(reportItemMap, key, ITEM_TYPE_LINE, null, itemData, null);
				registerItem(reportItemMap, key, ITEM_TYPE_ITEMDATA, null, itemData, diffAmount);

			} else if (orderAmount.compareTo(unitLoadAmount) < 0) {
				// more picked amount in packet than in order line
				// Not possible
				logger.log(Level.WARNING, logStr + "There is more amount in unit loads than order lines. itemData="
						+ itemData + ", unitLoadAmount=" + unitLoadAmount + ", orderLineAmount=" + orderAmount);
			}
		}

		List<DeliveryReportDto> reportItems = new ArrayList<>();
		reportItems.addAll(reportItemMap.values());
		Collections.sort(reportItems, new UnitLoadReportDtoComparator());

		int lineNumber = 0;
		ItemData itemData = null;
		for (DeliveryReportDto reportItem : reportItems) {
			if (itemData == null || !itemData.equals(reportItem.getItemData())) {
				itemData = reportItem.getItemData();
				lineNumber++;
			}
			reportItem.setLineNumber("" + lineNumber);
		}

		Map<String, Object> parameters = new HashMap<>();
		parameters.put("printDate", new Date());
		parameters.put("deliveryOrder", order);
		parameters.put("address", address);

		byte[] data = reportBusiness.createPdfDocument(client, "Deliverynote", Wms2BundleResolver.class, reportItems,
				parameters);

		Document doc = new Document();
		doc.setData(data);
		doc.setDocumentType(DocumentType.PDF);
		doc.setName("Deliverynote");

		return doc;
	}

	private void registerItems(Map<String, DeliveryReportDto> itemMap, UnitLoad unitLoad, Packet packet,
			boolean separateUnitLoad) {
		PickingOrder pickingOrder = null;
		DeliveryOrder deliveryOrder = null;

		String baseKey = "";
		UnitLoad registerUnitLoad = null;
		if (separateUnitLoad && unitLoad != null) {
			registerUnitLoad = unitLoad;
			baseKey += unitLoad.getId();
			DeliveryReportDto packetReportItem = registerItem(itemMap, baseKey, ITEM_TYPE_PACKET, unitLoad, null, null);
			packetReportItem.setExternalId(unitLoad.getExternalId());
		}
		if (packet != null) {
			pickingOrder = packet.getPickingOrder();
			deliveryOrder = packet.getDeliveryOrder();
		}

		List<StockUnit> stocksOnUnitLoad = stockUnitService.readByUnitLoad(unitLoad);
		for (StockUnit stock : stocksOnUnitLoad) {

			String key = baseKey + "-" + stock.getItemData().getId();

			registerItem(itemMap, key, ITEM_TYPE_LINE, registerUnitLoad, stock.getItemData(), null);

			DeliveryReportDto itemDatareportItem = registerItem(itemMap, key, ITEM_TYPE_ITEMDATA, registerUnitLoad,
					stock.getItemData(), stock.getAmount());

			if (deliveryOrder != null) {
				// Try to get some additional info of the single items out of the delivery order
				for (DeliveryOrderLine deliveryOrderLine : deliveryOrder.getLines()) {
					if (deliveryOrderLine.getItemData().equals(stock.getItemData())) {
						itemDatareportItem.setPickingHint(deliveryOrderLine.getPickingHint());
						itemDatareportItem.setPackingHint(deliveryOrderLine.getPackingHint());
						itemDatareportItem.setShippingHint(deliveryOrderLine.getShippingHint());
						itemDatareportItem.setUnitPrice(deliveryOrderLine.getUnitPrice());
					}
				}
			}
			if (pickingOrder != null) {
				// Try to get some additional info of the single items out of the picking order
				// The picking order trumps the delivery order
				for (PickingOrderLine pickingOrderLine : pickingOrder.getLines()) {
					if (pickingOrderLine.getItemData().equals(stock.getItemData())) {
						itemDatareportItem.setPickingHint(pickingOrderLine.getPickingHint());
						itemDatareportItem.setPackingHint(pickingOrderLine.getPackingHint());
						itemDatareportItem.setShippingHint(pickingOrderLine.getShippingHint());
						itemDatareportItem.setUnitPrice(pickingOrderLine.getUnitPrice());
					}
				}
			}

			Lot lot = stock.getLot();
			if (lot != null) {
				String lotKey = key + "-" + lot.getName().hashCode();
				DeliveryReportDto reportItem = registerItem(itemMap, lotKey, ITEM_TYPE_LOT, registerUnitLoad,
						stock.getItemData(), stock.getAmount());
				reportItem.setLotNumber(lot.getName());
			}
			if (lot != null && lot.getBestBeforeEnd() != null) {
				String bestBeforeKey = key + "-" + lot.getBestBeforeEnd().hashCode();
				DeliveryReportDto reportItem = registerItem(itemMap, bestBeforeKey, ITEM_TYPE_BESTBEFORE,
						registerUnitLoad, stock.getItemData(), stock.getAmount());
				reportItem.setBestBefore(lot.getBestBeforeEnd());
			}
			if (lot != null && lot.getBestBeforeEnd() != null) {
				String lotBestBeforeKey = key + "-" + lot.getName().hashCode() + "-"
						+ lot.getBestBeforeEnd().hashCode();
				DeliveryReportDto reportItem = registerItem(itemMap, lotBestBeforeKey, ITEM_TYPE_LOT_BESTBEFORE,
						registerUnitLoad, stock.getItemData(), stock.getAmount());
				reportItem.setLotNumber(lot.getName());
				reportItem.setBestBefore(lot.getBestBeforeEnd());
			}
			if (!StringUtils.isBlank(stock.getSerialNumber())) {
				String serialKey = key + "-" + stock.getSerialNumber().hashCode();
				DeliveryReportDto reportItem = registerItem(itemMap, serialKey, ITEM_TYPE_SERIAL, registerUnitLoad,
						stock.getItemData(), stock.getAmount());
				reportItem.setSerialNumber(stock.getSerialNumber());
			}
		}
	}

	private DeliveryReportDto registerItem(Map<String, DeliveryReportDto> itemMap, String key, String type,
			UnitLoad unitLoad, ItemData itemData, BigDecimal amount) {
		DeliveryReportDto value = itemMap.get(key + type);
		if (value == null) {
			value = new DeliveryReportDto(type, itemData, amount);
			value.setUnitLoad(unitLoad);
			if (unitLoad != null) {
				String label = unitLoad.getLabelId();
				String suffix = "-" + unitLoad.getId();
				value.setLabel(StringUtils.substringBefore(label, suffix));
			}
			itemMap.put(key + type, value);
		} else {
			value.addAmount(amount);
		}

		return value;
	}

	private void registerAmount(Map<ItemData, BigDecimal> registeredAmountMap, ItemData itemData, BigDecimal amount) {
		BigDecimal registeredAmount = registeredAmountMap.get(itemData);
		if (registeredAmount == null) {
			registeredAmountMap.put(itemData, amount);
		} else {
			registeredAmountMap.put(itemData, registeredAmount.add(amount));
		}
	}

	private static class UnitLoadReportDtoComparator implements Comparator<DeliveryReportDto>, Serializable {
		private static final long serialVersionUID = 1L;

		public int compare(DeliveryReportDto o1, DeliveryReportDto o2) {

			if (o1.getUnitLoad() != null && o2.getUnitLoad() != null) {
				int x = StringUtils.compare(o1.getUnitLoad().getLabelId(), o2.getUnitLoad().getLabelId());
				if (x != 0) {
					return x;
				}
			}

			if (o1.getItemData() != null && o2.getItemData() != null) {
				int x = StringUtils.compare(o1.getItemData().getNumber(), o2.getItemData().getNumber());
				if (x != 0) {
					return x;
				}
			}

			int x = StringUtils.compare(o1.getType(), o2.getType());
			if (x != 0) {
				return x;
			}

			x = StringUtils.compare(o1.getSerialNumber(), o2.getSerialNumber());
			if (x != 0) {
				return x;
			}

			x = StringUtils.compare(o1.getLotNumber(), o2.getLotNumber());
			if (x != 0) {
				return x;
			}

			if (o1.getAmount() != null && o2.getAmount() != null) {
				return o1.getAmount().compareTo(o2.getAmount());
			}

			return 0;
		}
	}
}
