package com.worldline.direct.widgets.returns.createreturnrequest;

import com.hybris.cockpitng.annotations.SocketEvent;
import com.worldline.direct.constants.WorldlinedirectcoreConstants;
import com.worldline.direct.enums.WeroRefundReason;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.core.model.order.payment.WorldlinePaymentInfoModel;
import de.hybris.platform.omsbackoffice.widgets.returns.createreturnrequest.CreateReturnRequestController;
import de.hybris.platform.omsbackoffice.widgets.returns.dtos.ReturnEntryToCreateDto;
import de.hybris.platform.payment.model.PaymentTransactionModel;
import de.hybris.platform.returns.model.RefundEntryModel;
import de.hybris.platform.returns.model.ReturnRequestModel;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Hbox;

public class WorldlineCreateReturnRequestController extends CreateReturnRequestController {

    @Wire
    private Combobox weroRefundReasonCombobox;

    @Wire
    private Hbox weroRefundReasonContainer;

    @Override
    @SocketEvent(socketId = "inputObject")
    public void initCreateReturnRequestForm(OrderModel inputObject) {
        super.initCreateReturnRequestForm(inputObject);
        initWeroRefundReason(inputObject);
    }

    private void initWeroRefundReason(OrderModel order) {
        boolean isWeroPayment = isWeroPaymentOrder(order);
        weroRefundReasonContainer.setVisible(isWeroPayment);

        if (isWeroPayment) {
            populateWeroRefundReasons();
        }
    }

    private boolean isWeroPaymentOrder(OrderModel order) {
        if (order == null || order.getPaymentTransactions() == null || order.getPaymentTransactions().isEmpty()) {
            return false;
        }
        PaymentTransactionModel lastTransaction = order.getPaymentTransactions()
                .get(order.getPaymentTransactions().size() - 1);
        if (lastTransaction.getInfo() instanceof WorldlinePaymentInfoModel) {
            WorldlinePaymentInfoModel paymentInfo = (WorldlinePaymentInfoModel) lastTransaction.getInfo();
            return WorldlinedirectcoreConstants.PAYMENT_METHOD_WERO == paymentInfo.getId().intValue();
        }
        return false;
    }

    private void populateWeroRefundReasons() {
        weroRefundReasonCombobox.getItems().clear();
        weroRefundReasonCombobox.setSelectedItem(null);

        for (WeroRefundReason reason : WeroRefundReason.values()) {
            Comboitem item = new Comboitem();
            String labelKey = "worldline.createreturnrequest.weroRefundReason." + reason.getCode();
            String label = this.getLabel(labelKey);
            item.setLabel(label != null && !label.equals(labelKey) ? label : reason.getCode());
            item.setValue(reason);
            weroRefundReasonCombobox.appendChild(item);
        }
    }

    @Override
    protected RefundEntryModel createRefundWithCustomAmount(ReturnRequestModel returnRequestModel,
                                                            ReturnEntryToCreateDto returnEntryToCreateDto) {
        RefundEntryModel refundEntry = super.createRefundWithCustomAmount(returnRequestModel, returnEntryToCreateDto);

        if (weroRefundReasonContainer.isVisible() && weroRefundReasonCombobox.getSelectedItem() != null) {
            WeroRefundReason selectedReason = weroRefundReasonCombobox.getSelectedItem().getValue();
            refundEntry.setWeroRefundReason(selectedReason);
            getModelService().save(refundEntry);
        }

        return refundEntry;
    }

    @Override
    public void reset() {
        super.reset();
        if (weroRefundReasonCombobox != null) {
            weroRefundReasonCombobox.setSelectedItem(null);
        }
    }
}
