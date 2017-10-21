package scripts.ShotdoxAPI.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.tribot.api.General;
import org.tribot.api2007.Banking;
import org.tribot.api2007.Equipment;
import org.tribot.api2007.GameTab;
import org.tribot.api2007.Equipment.SLOTS;
import org.tribot.api2007.Inventory;
import org.tribot.api2007.types.RSItem;
import org.tribot.script.Script;

public class ExtraInventory {
	private class SpecialItem {
		public Boolean isEquipment = false;
		public int id = 0;
		public int stackSize = 0;
		public Equipment.SLOTS inSlot = null;
		public Boolean isBeingMatched = false;

		public SpecialItem(Boolean isEquipmentArg, int idArg, int stackSizeArg) {
			isEquipment = isEquipmentArg;
			id = idArg;
			stackSize = stackSizeArg;
		}
	}

	private Boolean matchEquipmentSlots = false;
	private List<SpecialItem> items = new ArrayList<SpecialItem>();
	private final int INVENTORY_SIZE = 28;
	private final int SPAM_L_THAN = 5;
	private final int LOWER_SLEEP_MS = 800;
	private final int UPPER_SLEEP_MS = 1100;
	private Script script;

	/**
	 * An ExtraInvetory is an object that is used to describe an inventory and
	 * optional equipment setup, so that functions such as isSetupCorrect and
	 * bank can be use to quickly detect if the inventory the bot has is the
	 * same as the required inventory, and to be able to automatically bank the
	 * items it doesn't need, and withdraw (and equip) the items it does need.
	 */
	public ExtraInventory(Script sArg) {
		script = sArg;
	}

	/**
	 * @return true if the equipment slots should be set when banking and
	 *         testing the inventory
	 */
	public Boolean isMatchingEquipmentSlots() {
		return matchEquipmentSlots;
	}

	/**
	 * @param shouldMatch
	 *            is whether the equipment slots should be set when banking and
	 *            testing the inventory
	 */
	public void setMatchingEquipmentSlots(Boolean shouldMatch) {
		matchEquipmentSlots = shouldMatch;
	}

	/**
	 * Manually adds an item to the ExtraInventory, throws error if putting too
	 * much in inventory (>28)
	 * 
	 * @param itemToAdd
	 * @param amount
	 *            is how many of the item to add if it isn't stackable
	 * @param stackSize
	 *            is used for stackable items (runes, etc), amount should be 1
	 *            if item is stackable
	 * @return true
	 */
	public Boolean addItem(String itemToAdd, int amount, int stackSize) {
		if (amount > 1 && stackSize > 1 || stackSize == 0 || amount == 0) {
			return false;
		}
		
		script.println(nameToId(itemToAdd));
		return addItem(nameToId(itemToAdd), amount, stackSize);
	}

	/**
	 * Manually adds an item to the ExtraInventory, throws error if putting too
	 * much in inventory (>28)
	 * 
	 * @param itemIdToAdd
	 * @param amount
	 *            is how many of the item to add if it isn't stackable
	 * @param stackSize
	 *            is used for stackable items (runes, etc), amount should be 1
	 *            if item is stackable
	 * @return true
	 */
	public Boolean addItem(int itemIdToAdd, int amount, int stackSize) {
		if (amount > 1 && stackSize > 1 || stackSize == 0 || amount == 0) {
			return false;
		}

		int itemsInInventoryCount = 0;
		for (SpecialItem inventoryItem : items) {
			if (!inventoryItem.isEquipment) {
				itemsInInventoryCount++;
			}
		}

		if (itemsInInventoryCount + amount > INVENTORY_SIZE) {
			throw new java.lang.RuntimeException("Too many items added to inventory.");
		}

		for (int count = 0; count < amount; count++) {
			SpecialItem toAdd = new SpecialItem(false, itemIdToAdd, stackSize);
			items.add(toAdd);
		}

		return true;
	}

	/**
	 * Manually set one of the equipment slots to a specific item, will
	 * overwrite any item currently in that slot
	 * 
	 * @param slot
	 * @param itemToAdd (string)
	 * @param stackSize
	 *            used for stackable equipment (arrows), should be 1 otherwise
	 * @return true if the slot was successfully set to the id given
	 */
	public Boolean setSlot(Equipment.SLOTS slot, String itemToAdd, int stackSize){
		return  setSlot(slot, nameToId(itemToAdd), stackSize);
	}
	
	/**
	 * Manually set one of the equipment slots to a specific item, will
	 * overwrite any item currently in that slot
	 * 
	 * @param slot
	 * @param itemIdToAdd (int)
	 * @param stackSize
	 *            used for stackable equipment (arrows), should be 1 otherwise
	 * @return true if the slot was successfully set to the id given
	 */
	public Boolean setSlot(Equipment.SLOTS slot, int itemIdToAdd, int stackSize) {
		if (stackSize == 0) {
			return false;
		}

		for (SpecialItem itemToCheck : items) {
			if (itemToCheck.inSlot == slot) {
				items.remove(itemToCheck);
			}
		}

		SpecialItem toAdd = new SpecialItem(true, itemIdToAdd, stackSize);
		toAdd.inSlot = slot;
		items.add(toAdd);

		return true;
	}

	/**
	 * Set the current inventory as what the bot has in its (and equipped)
	 * 
	 * @param setEquipmentSlots
	 *            true if this ExtraInventory should match the equipment slots
	 *            false otherwise
	 * @return true if the inventory was succesfully set as the current bots
	 */
	public Boolean setAsCurrentInventory(Boolean setEquipmentSlots) {
		items = new ArrayList<SpecialItem>();
		matchEquipmentSlots = setEquipmentSlots;

		if (matchEquipmentSlots) {
			for (SLOTS slot : Equipment.SLOTS.values()) {
				RSItem item = Equipment.getItem(slot);
				if (item != null) {
					SpecialItem toAdd = new SpecialItem(true, item.getID(), item.getStack());
					toAdd.inSlot = slot;
					items.add(toAdd);
				}
			}
		}

		for (RSItem item : Inventory.getAll()) {
			SpecialItem toAdd = new SpecialItem(false, item.getID(), item.getStack());
			items.add(toAdd);
		}

		return true;
	}

	/**
	 * @return true if the bots current inventory and equipment completely
	 *         matches the extra inventory, false otherwise
	 */
	public Boolean isSetupCorrect() {

		if ((matchEquipmentSlots && !isEquipmentCorrect()) || !isInventoryCorrect()) {
			return false;
		}

		return true;
	}

	/**
	 * @return true if the bots current equipment completely matches the extra
	 *         equipment, false otherwise
	 */
	public Boolean isEquipmentCorrect() {
		if (!itemsMissingFromEquipment().isEmpty() || !itemsShouldntBeInEquipment().isEmpty()) {
			return false;
		}

		return true;
	}

	/**
	 * @return true if the bots current inventory completely matches the extra
	 *         inventory, false otherwise
	 */
	public Boolean isInventoryCorrect() {
		if (!itemsMissingFromInventory().isEmpty() || !itemsShouldntBeInInventory().isEmpty()) {
			return false;
		}

		return true;
	}

	/**
	 * This function is used when a bank is nearby and will automatically bank
	 * until the bots inventory (and equipment if matchEquipmentSlots is true)
	 * completely matches this ExtraInventory setup.
	 * 
	 * @return true if it was successful, false otherwise (timeout, not enough
	 *         items in bank, etc)
	 */
	public Boolean bank() {
		if (!equipItems()) {
			return false;
		}

		while (!isSetupCorrect()) {
			if (!itemsShouldntBeInInventory().isEmpty()) {
				if (!myOpenBank()) {
					return false;
				}

				if (!depositInventoryItems()) {
					return false;
				}

				continue;
			}

			if (matchEquipmentSlots) {
				int freeInventoryRequired = Math.max(itemsMissingFromEquipment().size(),
						itemsShouldntBeInEquipment().size());

				while (freeInventoryRequired > INVENTORY_SIZE - Inventory.getAll().length) {
					if (!myOpenBank()) {
						return false;
					}

					Banking.depositItem(Inventory.getAll()[0], 0);

					General.sleep(LOWER_SLEEP_MS, UPPER_SLEEP_MS);
				}

				if (!itemsShouldntBeInEquipment().isEmpty()) {
					if (!myCloseBank()) {
						return false;
					}

					if (!depositEquipedItems()) {
						return false;
					}

					continue;
				}

				if (!itemsMissingFromEquipment().isEmpty()) {
					if (!myOpenBank()) {
						return false;
					}

					if (!withdrawEquipment()) {
						return false;
					}

					continue;
				}

			}

			if (!itemsMissingFromInventory().isEmpty()) {

				if (!myOpenBank()) {
					return false;
				}

				if (!withdrawInventory()) {
					return false;
				}

				continue;
			}
		}

		return isSetupCorrect();
	}

	/**
	 * @return the string of the current inventory with the correct format for
	 *         using in fromString later useful for script.println(...) to get
	 *         the string and copy that directly into code
	 */
	public String inventoryToString() {
		return listToString(items);
	}

	/**
	 * Will set the ExtraInventory from a saved string. This string must take
	 * the format "isEquipment:id:stackSize(:slot)-" for each item where () is
	 * optional parameters if isEquipment is true, will throw error if format
	 * not correct
	 * 
	 * @param itemString
	 * @return true if the inventory was successfully set
	 */
	public Boolean fromString(String itemString) {
		matchEquipmentSlots = false;
		String[] itemStrings = itemString.split("-");
		try {
			for (int itemNo = 0; itemNo < itemStrings.length; itemNo++) {
				String[] itemStringArray = itemStrings[itemNo].split(":");

				if (Boolean.parseBoolean(itemStringArray[0])) {
					setSlot(Equipment.SLOTS.valueOf(itemStringArray[3]), Integer.parseInt(itemStringArray[1]),
							Integer.parseInt(itemStringArray[2]));
				} else {
					addItem(Integer.parseInt(itemStringArray[1]), 1, Integer.parseInt(itemStringArray[2]));
				}
			}
		} catch (Exception exception) {
			throw new java.lang.RuntimeException("Error in inventory string format");
		}

		return true;
	}

	private List<SpecialItem> itemsMissingFromInventory() {
		List<SpecialItem> missingFromInventory = new ArrayList<SpecialItem>();
		if (matchEquipmentSlots) {

		}

		for (RSItem item : Inventory.getAll()) {
			for (SpecialItem itemToCheck : items) {
				if (item.getID() == itemToCheck.id && item.getStack() <= itemToCheck.stackSize
						&& !itemToCheck.isBeingMatched && !itemToCheck.isEquipment) {
					if (item.getStack() < itemToCheck.stackSize) {
						missingFromInventory
								.add(new SpecialItem(false, item.getID(), itemToCheck.stackSize - item.getStack()));
					}
					itemToCheck.isBeingMatched = true;
					break;
				}
			}
		}

		for (SpecialItem itemToCheck : items) {
			if (!itemToCheck.isBeingMatched && !itemToCheck.isEquipment) {
				missingFromInventory.add(itemToCheck);
			}
			itemToCheck.isBeingMatched = false;
		}

		return missingFromInventory;
	}

	private List<SpecialItem> itemsShouldntBeInInventory() {
		List<SpecialItem> shouldntBeInInventory = new ArrayList<SpecialItem>();

		for (RSItem item : Inventory.getAll()) {
			Boolean matched = false;
			for (SpecialItem itemToCheck : items) {
				if (item.getID() == itemToCheck.id && !itemToCheck.isEquipment && !itemToCheck.isBeingMatched) {
					if (item.getStack() > itemToCheck.stackSize) {
						shouldntBeInInventory
								.add(new SpecialItem(false, item.getID(), item.getStack() - itemToCheck.stackSize));
					}
					itemToCheck.isBeingMatched = true;
					matched = true;
					break;
				}
			}
			if (!matched) {
				shouldntBeInInventory.add(new SpecialItem(false, item.getID(), item.getStack()));
			}
		}

		for (SpecialItem itemToCheck : items) {
			itemToCheck.isBeingMatched = false;
		}

		return shouldntBeInInventory;
	}

	private List<SpecialItem> itemsMissingFromEquipment() {
		List<SpecialItem> missingFromEquipment = new ArrayList<SpecialItem>();

		for (SLOTS slot : Equipment.SLOTS.values()) {
			RSItem item = Equipment.getItem(slot);
			if (item != null) {
				for (SpecialItem itemToCheck : items) {
					if (item.getID() == itemToCheck.id && item.getStack() <= itemToCheck.stackSize
							&& itemToCheck.isEquipment && itemToCheck.inSlot == slot && !itemToCheck.isBeingMatched) {
						if (item.getStack() < itemToCheck.stackSize) {
							SpecialItem incorectItem = new SpecialItem(true, item.getID(),
									itemToCheck.stackSize - item.getStack());
							incorectItem.inSlot = slot;
							missingFromEquipment.add(incorectItem);
						}
						itemToCheck.isBeingMatched = true;
						break;
					}
				}
			}
		}

		for (SpecialItem itemToCheck : items) {
			if (!itemToCheck.isBeingMatched && itemToCheck.isEquipment) {
				missingFromEquipment.add(itemToCheck);
			}
			itemToCheck.isBeingMatched = false;
		}

		return missingFromEquipment;
	}

	private List<SpecialItem> itemsShouldntBeInEquipment() {
		List<SpecialItem> shouldntBeInEquipment = new ArrayList<SpecialItem>();

		for (SLOTS slot : Equipment.SLOTS.values()) {
			Boolean matched = false;
			RSItem item = Equipment.getItem(slot);
			if (item != null) {
				for (SpecialItem itemToCheck : items) {
					if (item.getID() == itemToCheck.id && itemToCheck.inSlot == slot && !itemToCheck.isBeingMatched) {
						if (item.getStack() > itemToCheck.stackSize) {
							SpecialItem incorectItem = new SpecialItem(true, item.getID(), item.getStack());
							incorectItem.inSlot = slot;
							shouldntBeInEquipment.add(incorectItem);
						}
						itemToCheck.isBeingMatched = true;
						matched = true;
						break;
					}
				}
				if (!matched) {
					SpecialItem incorectItem = new SpecialItem(true, item.getID(), item.getStack());
					incorectItem.inSlot = slot;
					shouldntBeInEquipment.add(incorectItem);
				}
			}

		}

		for (SpecialItem itemToCheck : items) {
			itemToCheck.isBeingMatched = false;
		}

		return shouldntBeInEquipment;
	}

	private Boolean depositInventoryItems() {
		List<SpecialItem> itemsToDeposit = new ArrayList<SpecialItem>();
		while (!(itemsToDeposit = itemsShouldntBeInInventory()).isEmpty()) {

			List<Integer> depositedIds = new ArrayList<Integer>();

			for (SpecialItem itemToDeposit : itemsToDeposit) {
				if (depositedIds.contains(itemToDeposit.id)) {
					continue;
				}

				int amountToDeposit = 0;
				for (SpecialItem itemToCheck : itemsToDeposit) {
					if (itemToCheck.id == itemToDeposit.id) {
						amountToDeposit++;
					}
				}

				if (itemToDeposit.stackSize > 1) {
					amountToDeposit = itemToDeposit.stackSize;
				}

				if (Inventory.getCount(itemToDeposit.id) == amountToDeposit) {
					if (!Banking.deposit(0, itemToDeposit.id)) {
						return false;
					}
				} else {
					if (!Banking.deposit(amountToDeposit, itemToDeposit.id)) {
						return false;
					}
				}

				depositedIds.add(itemToDeposit.id);
			}

			General.sleep(LOWER_SLEEP_MS, UPPER_SLEEP_MS);
		}
		return true;
	}

	private Boolean depositEquipedItems() {
		if (GameTab.getOpen() != GameTab.TABS.EQUIPMENT) {
			GameTab.open(GameTab.TABS.EQUIPMENT);
		}

		List<SpecialItem> itemsToDequip = new ArrayList<SpecialItem>();
		while (!(itemsToDequip = itemsShouldntBeInEquipment()).isEmpty()) {
			for (SpecialItem itemToDequip : itemsToDequip) {
				Equipment.remove(itemToDequip.inSlot);
			}

			General.sleep(LOWER_SLEEP_MS, UPPER_SLEEP_MS);
		}

		return true;
	}

	private Boolean withdrawEquipment() {
		List<SpecialItem> itemsToEquip = new ArrayList<SpecialItem>();
		while (!(itemsToEquip = itemsMissingFromEquipment()).isEmpty()) {

			if (!myOpenBank()) {
				return false;
			}

			for (SpecialItem itemToEquip : itemsToEquip) {

				RSItem[] inBank = Banking.find(itemToEquip.id);
				if (inBank == null || (inBank).length == 0 || inBank[0].getStack() < itemToEquip.stackSize) {
					return false;
				}

				if (!Banking.withdraw(itemToEquip.stackSize, itemToEquip.id)) {
					return false;
				}
			}

			if (!myCloseBank()) {
				return false;
			}

			if (!equipItems()) {
				return false;
			}
		}

		return true;
	}

	private Boolean withdrawInventory() {
		List<SpecialItem> itemsToWithdraw = new ArrayList<SpecialItem>();
		List<Integer> withdrawnIds = new ArrayList<Integer>();

		while (!(itemsToWithdraw = itemsMissingFromInventory()).isEmpty()) {
			for (SpecialItem itemToWithdraw : itemsToWithdraw) {

				if (withdrawnIds.contains(itemToWithdraw.id)) {
					continue;
				}

				int amountToWithdraw = 0;
				for (SpecialItem itemToCheck : itemsToWithdraw) {
					if (itemToCheck.id == itemToWithdraw.id) {
						amountToWithdraw++;
					}
				}

				RSItem[] inBank = Banking.find(itemToWithdraw.id);
				if (inBank == null || (inBank).length == 0
						|| ((amountToWithdraw == 1 && inBank[0].getStack() < itemToWithdraw.stackSize)
								|| (amountToWithdraw > 1 && inBank[0].getStack() < amountToWithdraw))) {

					return false;
				}

				if (itemToWithdraw.stackSize > 1 && !Banking.withdraw(itemToWithdraw.stackSize, itemToWithdraw.id)) {
					return false;
				}

				if (itemToWithdraw.stackSize == 1 && amountToWithdraw < SPAM_L_THAN) {
					for (int count = 0; count < amountToWithdraw; count++) {
						if (!Banking.withdraw(1, itemToWithdraw.id)) {
							return false;
						}
					}
				}

				if (itemToWithdraw.stackSize == 1 && amountToWithdraw >= SPAM_L_THAN
						&& !Banking.withdraw(amountToWithdraw, itemToWithdraw.id)) {
					return false;
				}

				withdrawnIds.add(itemToWithdraw.id);
			}

			General.sleep(LOWER_SLEEP_MS, UPPER_SLEEP_MS);
		}

		return true;
	}

	private Boolean equipItems() {
		for (SpecialItem itemToWear : itemsShouldntBeInInventory()) {
			for (SpecialItem itemMissing : itemsMissingFromEquipment()) {
				if (itemToWear.id == itemMissing.id) {
					if (GameTab.getOpen() != GameTab.TABS.INVENTORY) {
						GameTab.open(GameTab.TABS.INVENTORY);
					}

					RSItem toEquip = Inventory.find(itemToWear.id)[0];

					if (toEquip == null) {
						return false;
					}
					if (!toEquip.click()) {
						return false;
					}
				}
			}
		}

		General.sleep(LOWER_SLEEP_MS, UPPER_SLEEP_MS);

		return true;
	}

	private Boolean myOpenBank() {
		int timeout = 10;
		while (!Banking.isBankScreenOpen()) {
			timeout--;
			if (timeout == 0) {
				return false;
			}
			Banking.openBank();
			General.sleep(LOWER_SLEEP_MS, UPPER_SLEEP_MS);
		}
		return true;
	}

	private Boolean myCloseBank() {
		int timeout = 10;
		while (Banking.isBankScreenOpen()) {
			timeout--;
			if (timeout == 0) {
				return false;
			}
			Banking.close();
			General.sleep(LOWER_SLEEP_MS, UPPER_SLEEP_MS);
		}
		return true;
	}

	private String listToString(List<SpecialItem> thisList) {
		String toReturn = "";

		for (SpecialItem item : thisList) {
			toReturn = toReturn.concat(Boolean.toString(item.isEquipment));
			toReturn = toReturn.concat(":");
			toReturn = toReturn.concat(Integer.toString(item.id));
			toReturn = toReturn.concat(":");
			toReturn = toReturn.concat(Integer.toString(item.stackSize));
			if (item.isEquipment) {
				toReturn = toReturn.concat(":");
				toReturn = toReturn.concat(slotToString(item.inSlot));
			}
			toReturn = toReturn.concat("|");
		}

		return toReturn;
	}

	private String slotToString(Equipment.SLOTS slot) {
		String slotString = "";
		switch (slot) {
		case AMULET:
			slotString = "AMULET";
			break;
		case ARROW:
			slotString = "ARROW";
			break;
		case BODY:
			slotString = "BODY";
			break;
		case BOOTS:
			slotString = "BOOTS";
			break;
		case CAPE:
			slotString = "CAPE";
			break;
		case GLOVES:
			slotString = "GLOVES";
			break;
		case HELMET:
			slotString = "HELMET";
			break;
		case LEGS:
			slotString = "LEGS";
			break;
		case RING:
			slotString = "RING";
			break;
		case SHIELD:
			slotString = "SHIELD";
			break;
		case WEAPON:
			slotString = "WEAPON";
			break;
		default:
			return null;
		}

		return slotString;
	}

	private int nameToId(String name) {
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(System.getProperty("user.dir") + "/bin/scripts/ShotdoxAPI/util/names.txt"));
		    StringBuilder sb = new StringBuilder();
		    String line = br.readLine();

		    while (line != null) {
		        sb.append(line);
		        sb.append(System.lineSeparator());
		        line = br.readLine();
		    }
		    String[] everything = sb.toString().split("}");
		    for (String item: everything){
		    	if (item.toLowerCase().contains(name.toLowerCase())){
		    		if (item.split("\"")[7].toLowerCase().equals(name.toLowerCase())){
		    			br.close();
		    			return Integer.parseInt(item.split("\"")[1]);
		    		}
		    	}
		    }
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return 0;
	}
}
