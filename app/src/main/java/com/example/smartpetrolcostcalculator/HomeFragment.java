package com.example.smartpetrolcostcalculator;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Locale;

public class HomeFragment extends Fragment {

    private static final double BUDI_SUBSIDY_RATE = 1.99;

    private Spinner spinnerPetrolType;
    private MaterialButtonToggleGroup toggleInputMode;
    private TextInputLayout tilMainInput;
    private TextInputEditText etMainInput, etPetrolPrice;
    private MaterialSwitch switchBudi;
    private Button btnCalculate;

    private TextView tvTotalCost, tvTotalCostLabel;
    private View cardSummary;
    private TextView tvSummaryLiters, tvSummaryPumpPrice, tvSummaryBudiPrice;

    public HomeFragment() {
        // Required empty constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        spinnerPetrolType = view.findViewById(R.id.spinnerPetrolType);
        toggleInputMode = view.findViewById(R.id.toggleInputMode);
        tilMainInput = view.findViewById(R.id.tilMainInput);
        etMainInput = view.findViewById(R.id.etMainInput);
        etPetrolPrice = view.findViewById(R.id.etPetrolPrice);
        switchBudi = view.findViewById(R.id.switchBudi);
        btnCalculate = view.findViewById(R.id.btnCalculate);

        tvTotalCostLabel = view.findViewById(R.id.tvTotalCostLabel);
        tvTotalCost = view.findViewById(R.id.tvTotalCost);

        cardSummary = view.findViewById(R.id.cardSummary);
        tvSummaryLiters = view.findViewById(R.id.tvSummaryLiters);
        tvSummaryPumpPrice = view.findViewById(R.id.tvSummaryPumpPrice);
        tvSummaryBudiPrice = view.findViewById(R.id.tvSummaryBudiPrice);

        setupPetrolSpinner();

        toggleInputMode.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnModeLiters) {
                    tilMainInput.setHint("Total Fuel (Liters)");
                    tvTotalCostLabel.setText("Estimated Final Cost");
                } else {
                    tilMainInput.setHint("Total Spend (RM)");
                    tvTotalCostLabel.setText("Total Spend");
                }

                etMainInput.setText("");
                cardSummary.setVisibility(View.GONE);
                tvTotalCost.setText("RM 0.00");
            }
        });

        btnCalculate.setOnClickListener(v -> calculatePetrolCost());

        switchBudi.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!TextUtils.isEmpty(etMainInput.getText()) && !TextUtils.isEmpty(etPetrolPrice.getText())) {
                calculatePetrolCost();
            }
        });

        return view;
    }

    private void setupPetrolSpinner() {
        String[] petrolTypes = {"RON95", "RON97", "Diesel"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                petrolTypes
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPetrolType.setAdapter(adapter);
    }

    private void calculatePetrolCost() {
        String inputText = etMainInput.getText().toString().trim();
        String priceText = etPetrolPrice.getText().toString().trim();

        if (TextUtils.isEmpty(inputText)) {
            etMainInput.setError("Required");
            etMainInput.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(priceText)) {
            etPetrolPrice.setError("Required");
            etPetrolPrice.requestFocus();
            return;
        }

        try {
            double inputValue = Double.parseDouble(inputText);
            double petrolPrice = Double.parseDouble(priceText);

            if (petrolPrice <= 0 || inputValue < 0) {
                Toast.makeText(requireContext(), "Please enter valid numbers > 0", Toast.LENGTH_SHORT).show();
                return;
            }

            String petrolType = spinnerPetrolType.getSelectedItem().toString();
            boolean isEligible = switchBudi.isChecked();

            double fuelUsage = 0;
            double totalPetrolCost = 0;

            // --- CORRECTED LOGIC ---
            if (toggleInputMode.getCheckedButtonId() == R.id.btnModeLiters) {
                // User enters Liters, we calculate the cost normally
                fuelUsage = inputValue;
                totalPetrolCost = fuelUsage * petrolPrice;
            } else {
                // User enters RM, we reverse-calculate the Liters based on the EFFECTIVE price
                double effectivePricePerLiter = petrolPrice;

                if (petrolType.equals("RON95") && isEligible) {
                    effectivePricePerLiter = petrolPrice - BUDI_SUBSIDY_RATE;
                    // Prevent app crash if pump price is weirdly lower than subsidy
                    if (effectivePricePerLiter <= 0) effectivePricePerLiter = 0.01;
                }

                // Your RM now buys more liters because the effective price is lower!
                fuelUsage = inputValue / effectivePricePerLiter;
                totalPetrolCost = fuelUsage * petrolPrice; // Calculate standard cost to show the full receipt
            }

            double budiRebate = 0.00;

            if (petrolType.equals("RON95") && isEligible) {
                budiRebate = fuelUsage * BUDI_SUBSIDY_RATE;
                tvSummaryBudiPrice.setText(String.format(Locale.getDefault(), "RM %.2f/L", BUDI_SUBSIDY_RATE));
            } else if (!petrolType.equals("RON95") && isEligible) {
                Toast.makeText(requireContext(), "BUDI MADANI is strictly for RON95 users.", Toast.LENGTH_SHORT).show();
                switchBudi.setChecked(false);
                tvSummaryBudiPrice.setText("Not Eligible");
            } else {
                tvSummaryBudiPrice.setText("Not Applied");
            }

            double finalPayable = totalPetrolCost - budiRebate;
            if (finalPayable < 0) finalPayable = 0;

            tvTotalCost.setText(String.format(Locale.getDefault(), "RM %.2f", finalPayable));

            tvSummaryLiters.setText(String.format(Locale.getDefault(), "%.3f L", fuelUsage));
            tvSummaryPumpPrice.setText(String.format(Locale.getDefault(), "RM %.2f/L", petrolPrice));
            cardSummary.setVisibility(View.VISIBLE);

            if (budiRebate > 0) {
                Toast.makeText(requireContext(),
                        String.format(Locale.getDefault(), "Subsidy Applied! You saved RM %.2f", budiRebate),
                        Toast.LENGTH_LONG).show();
            }

        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Invalid number format", Toast.LENGTH_SHORT).show();
        }
    }
}