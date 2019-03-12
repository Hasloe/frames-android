package com.checkout.sdk.cardinput

import android.content.Context
import android.graphics.drawable.Drawable
import android.support.design.widget.TextInputLayout
import android.text.Editable
import android.text.InputFilter
import android.text.SpannableStringBuilder
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View.OnFocusChangeListener
import com.checkout.sdk.R
import com.checkout.sdk.architecture.MvpView
import com.checkout.sdk.architecture.PresenterStore
import com.checkout.sdk.store.DataStore
import com.checkout.sdk.store.InMemoryStore
import com.checkout.sdk.utils.AfterTextChangedListener
import com.checkout.sdk.utils.CardUtils
import kotlinx.android.synthetic.main.view_card_input.view.*

/**
 * <h1>CardInput class</h1>
 * The CardInput class has the purpose extending an AppCompatEditText and provide validation
 * and formatting for the user's card details.
 *
 *
 * This class will validate on the "afterTextChanged" event and display a card icon on the right
 * side based on  the users input. It will also span spaces following the [CardUtils] details.
 */
class CardInput @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    TextInputLayout(context, attrs),
    MvpView<CardInputUiState> {


    private val inMemoryStore = InMemoryStore.Factory.get()
    private var mCardInputListener: Listener? = null
    private lateinit var presenter: CardInputPresenter

    init {
        LayoutInflater.from(context).inflate(R.layout.view_card_input, this)
    }

    /**
     * The UI initialisation
     *
     *
     * Used to initialise element as well as setting up appropriate listeners
     */
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        // Create/get and start the presenter
        presenter = PresenterStore.getOrCreate(
            CardInputPresenter::class.java,
            { CardInputPresenter() })

        presenter.start(this)

        // Add listener for text input
        card_input_edit_text.addTextChangedListener(object : AfterTextChangedListener() {
            override fun afterTextChanged(text: Editable) {
                val cardInputUseCase = CardInputUseCase(text, inMemoryStore)
                presenter.textChanged(cardInputUseCase)
            }
        })

        // When the CardInput loses focus check if the card number is not valid and trigger an error
        onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
            val cardFocusUseCase = CardFocusUseCase(hasFocus, DataStore.getInstance())
            presenter.focusChanged(cardFocusUseCase)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        presenter.stop()
    }

    override fun onStateUpdated(uiState: CardInputUiState) {
        // Get Card type
        card_input_edit_text.filters =
                arrayOf<InputFilter>(InputFilter.LengthFilter(uiState.cardType.maxCardLength))
        // Set the CardInput icon based on the type of card
        setCardTypeIcon(uiState.cardType)

        restoreCardNumberIfNecessary(uiState)

        showOrClearErrors(uiState)
    }

    /**
     * This method will display a card icon associated to the specific card scheme
     */
    private fun setCardTypeIcon(type: CardUtils.Cards) {
        val img: Drawable
        if (type.resourceId != 0) {
            img = context.resources.getDrawable(type.resourceId)
            img.setBounds(0, 0, 68, 68)
            card_input_edit_text.setCompoundDrawables(null, null, img, null)
            card_input_edit_text.compoundDrawablePadding = 5
        } else {
            card_input_edit_text.setCompoundDrawables(null, null, null, null)
        }
    }

    private fun restoreCardNumberIfNecessary(cardInputResult: CardInputUiState) {
        if (card_input_edit_text.text.isEmpty() && cardInputResult.cardNumber.isNotEmpty()) {
            card_input_edit_text.setText(cardInputResult.cardNumber)
            card_input_edit_text.setSelection(cardInputResult.cardNumber.length)
            val cardInputUseCase = CardInputUseCase(card_input_edit_text.text, inMemoryStore)
            presenter.textChanged(cardInputUseCase)
        }
    }

    private fun showOrClearErrors(uiState: CardInputUiState) {
        if (uiState.showCardError) {
            error = resources.getString(R.string.error_card_number)
            isErrorEnabled = true
        } else {
            isErrorEnabled = false
        }
    }

    /**
     * Used to set the callback listener for when the card input is completed
     */
    fun setCardListener(listener: Listener) {
        this.mCardInputListener = listener
    }

    /**
     * Clear all the ui and backing values
     */
    fun clear() {
        card_input_edit_text.text = SpannableStringBuilder("")
    }

    /**
     * An interface needed to communicate with the parent once the field is successfully completed
     */
    interface Listener {
        fun onCardInputFinish(number: String)

        fun onCardError()

        fun onClearCardError()
    }
}
