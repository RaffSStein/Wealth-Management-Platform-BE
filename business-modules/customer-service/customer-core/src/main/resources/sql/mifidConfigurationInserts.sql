--TODO: answer scores (now fixed at 10) and risk profile configurations to be defined
-- Insert Mifid Questionnaire
INSERT INTO mifid_questionnaire (name, valid_from, valid_to, description, questionnaire_version, status)
VALUES ('Mifid 2025', '2025-01-01', '2025-12-31', 'Standard Mifid questionnaire for 2025', '1.0', 'ACTIVE');

-- Insert Sections - 5 sections: Personal Information, Investment Experience, Financial Situation, Risk Tolerance, ESG Preferences
INSERT INTO mifid_section (title, order_index, questionnaire_id)
VALUES ('Personal Information', 1, (SELECT id FROM mifid_questionnaire WHERE name = 'Mifid 2025'));
INSERT INTO mifid_section (title, order_index, questionnaire_id)
VALUES ('Investment Experience', 2, (SELECT id FROM mifid_questionnaire WHERE name = 'Mifid 2025'));
INSERT INTO mifid_section (title, order_index, questionnaire_id)
VALUES ('Financial Situation', 3, (SELECT id FROM mifid_questionnaire WHERE name = 'Mifid 2025'));
INSERT INTO mifid_section (title, order_index, questionnaire_id)
VALUES ('Risk Tolerance', 4, (SELECT id FROM mifid_questionnaire WHERE name = 'Mifid 2025'));
INSERT INTO mifid_section (title, order_index, questionnaire_id)
VALUES ('ESG Preferences', 5, (SELECT id FROM mifid_questionnaire WHERE name = 'Mifid 2025'));

-- questions for Personal Information
INSERT INTO mifid_question (text, question_type, is_required, order_index, code, section_id)
VALUES ('What is your annual income?', 'NUMERIC', true, 1, 'PI-001', (SELECT id FROM mifid_section WHERE title = 'Personal Information'));
INSERT INTO mifid_question (text, question_type, is_required, order_index, code, section_id)
VALUES ('What is your employment status?', 'SINGLE', true, 2, 'PI-002', (SELECT id FROM mifid_section WHERE title = 'Personal Information'));
INSERT INTO mifid_question (text, question_type, is_required, order_index, code, section_id)
VALUES ('What is your highest level of education?', 'SINGLE', true, 3, 'PI-003', (SELECT id FROM mifid_section WHERE title = 'Personal Information'));

-- answer options for Personal Information
-- PI-001 (numeric free value)
INSERT INTO mifid_answer_option (option_text, score, order_index, code, question_id)
VALUES ('Free numeric value', 5, 1, 'PI-001-OPT1', (SELECT id FROM mifid_question WHERE code = 'PI-001'));

-- PI-002
INSERT INTO mifid_answer_option (option_text, score, order_index, code, question_id)
VALUES ('Employed full-time', 10, 1, 'PI-002-OPT1', (SELECT id FROM mifid_question WHERE code = 'PI-002'));
INSERT INTO mifid_answer_option (option_text, score, order_index, code, question_id)
VALUES ('Employed part-time', 7, 2, 'PI-002-OPT2', (SELECT id FROM mifid_question WHERE code = 'PI-002'));
INSERT INTO mifid_answer_option (option_text, score, order_index, code, question_id)
VALUES ('Self-employed', 8, 3, 'PI-002-OPT3', (SELECT id FROM mifid_question WHERE code = 'PI-002'));
INSERT INTO mifid_answer_option (option_text, score, order_index, code, question_id)
VALUES ('Unemployed', 3, 4, 'PI-002-OPT4', (SELECT id FROM mifid_question WHERE code = 'PI-002'));
-- PI-003
INSERT INTO mifid_answer_option (option_text, score, order_index, code, question_id)
VALUES ('High school or lower', 5, 1, 'PI-003-OPT1', (SELECT id FROM mifid_question WHERE code = 'PI-003'));
INSERT INTO mifid_answer_option (option_text, score, order_index, code, question_id)
VALUES ('Bachelor''s degree', 7, 2, 'PI-003-OPT2', (SELECT id FROM mifid_question WHERE code = 'PI-003'));
INSERT INTO mifid_answer_option (option_text, score, order_index, code, question_id)
VALUES ('Master''s degree', 9, 3, 'PI-003-OPT3', (SELECT id FROM mifid_question WHERE code = 'PI-003'));
INSERT INTO mifid_answer_option (option_text, score, order_index, code, question_id)
VALUES ('Doctorate', 10, 4, 'PI-003-OPT4', (SELECT id FROM mifid_question WHERE code = 'PI-003'));


-- questions for Investment Experience
INSERT INTO mifid_question (text, question_type, is_required, order_index, code, section_id)
VALUES ('Can the capital invested in a financial product issued by a bank be reduced or eliminated?', 'SINGLE', true, 1, 'IE-001', (SELECT id FROM mifid_section WHERE title = 'Investment Experience'));

INSERT INTO mifid_question (text, question_type, is_required, order_index, code, section_id)
VALUES ('What is the effect of leverage on an investment?', 'SINGLE', true, 1, 'IE-002', (SELECT id FROM mifid_section WHERE title = 'Investment Experience'));

INSERT INTO mifid_question (text, question_type, is_required, order_index, code, section_id)
VALUES ('What are the derivative instruments?', 'SINGLE', true, 1, 'IE-003', (SELECT id FROM mifid_section WHERE title = 'Investment Experience'));

-- answer options for Investment Experience
-- question IE-001
INSERT INTO mifid_answer_option (option_text, score, order_index, code, question_id)
VALUES ('No, as financial support is always provided for banking intermediaries in crisis.', 10, 1, 'IE-001-OPT1', (SELECT id FROM mifid_question WHERE code = 'IE-001'));

INSERT INTO mifid_answer_option (option_text, score, order_index, code, question_id)
VALUES ('No because banks can''t fail.', 10, 2, 'IE-001-OPT2', (SELECT id FROM mifid_question WHERE code = 'IE-001'));

INSERT INTO mifid_answer_option (option_text, score, order_index, code, question_id)
VALUES ('Yes, in the event of a bank failure or risk of failure.', 10, 3, 'IE-001-OPT3', (SELECT id FROM mifid_question WHERE code = 'IE-001'));

INSERT INTO mifid_answer_option (option_text, score, order_index, code, question_id)
VALUES ('I do not know.', 10, 4, 'IE-001-OPT4', (SELECT id FROM mifid_question WHERE code = 'IE-001'));

-- question IE-002
INSERT INTO mifid_answer_option (option_text, score, order_index, code, question_id)
VALUES ('It allows you to make a gain when a specific event occurs (such as an increase in the price of a related security/index, change in interest rates, etc.)', 10, 1, 'IE-002-OPT1', (SELECT id FROM mifid_question WHERE code = 'IE-002'));
INSERT INTO mifid_answer_option (option_text, score, order_index, code, question_id)
VALUES ('The leverage effect allows you to invest in foreign markets that would otherwise be inaccessible.', 10, 2, 'IE-002-OPT2', (SELECT id FROM mifid_question WHERE code = 'IE-002'));
INSERT INTO mifid_answer_option (option_text, score, order_index, code, question_id)
VALUES ('It amplifies the potential gain/loss and consequently the risk level of the investment.', 10, 3, 'IE-002-OPT3', (SELECT id FROM mifid_question WHERE code = 'IE-002'));
INSERT INTO mifid_answer_option (option_text, score, order_index, code, question_id)
VALUES ('It is the operating mechanism of repurchase agreements,', 10, 4, 'IE-002-OPT4', (SELECT id FROM mifid_question WHERE code = 'IE-002'));

-- question IE-003
INSERT INTO mifid_answer_option (option_text, score, order_index, code, question_id)
VALUES ('They are financial instruments whose redemption value is always guaranteed.', 10, 1, 'IE-003-OPT1', (SELECT id FROM mifid_question WHERE code = 'IE-003'));
INSERT INTO mifid_answer_option (option_text, score, order_index, code, question_id)
VALUES ('They are financial instruments whose value depends on the performance of the underlying investment and may result in losses exceeding the invested capital.', 10, 2, 'IE-003-OPT2', (SELECT id FROM mifid_question WHERE code = 'IE-003'));
INSERT INTO mifid_answer_option (option_text, score, order_index, code, question_id)
VALUES ('They are insurance policies intended for institutional investors.', 10, 3, 'IE-003-OPT3', (SELECT id FROM mifid_question WHERE code = 'IE-003'));
INSERT INTO mifid_answer_option (option_text, score, order_index, code, question_id)
VALUES ('They are instruments that allow investment in unlisted shares.', 10, 4, 'IE-003-OPT4', (SELECT id FROM mifid_question WHERE code = 'IE-003'));

-- questions for Financial Situation
INSERT INTO mifid_question (text, question_type, is_required, order_index, code, section_id)
VALUES ('What is your current employment status?', 'SINGLE', true, 1, 'FS-001', (SELECT id FROM mifid_section WHERE title = 'Financial Situation'));
INSERT INTO mifid_question (text, question_type, is_required, order_index, code, section_id)
VALUES ('Do you have any outstanding loans or debts?', 'SINGLE', true, 2, 'FS-002', (SELECT id FROM mifid_section WHERE title = 'Financial Situation'));
INSERT INTO mifid_question (text, question_type, is_required, order_index, code, section_id)
VALUES ('What is your monthly savings rate?', 'SINGLE', true, 3, 'FS-003', (SELECT id FROM mifid_section WHERE title = 'Financial Situation'));

-- answer options for Financial Situation
-- FS-001
INSERT INTO mifid_answer_option (option_text, score, order_index, code, question_id)
VALUES ('Employed full-time', 10, 1, 'FS-001-OPT1', (SELECT id FROM mifid_question WHERE code = 'FS-001'));
INSERT INTO mifid_answer_option (option_text, score, order_index, code, question_id)
VALUES ('Employed part-time', 7, 2, 'FS-001-OPT2', (SELECT id FROM mifid_question WHERE code = 'FS-001'));
INSERT INTO mifid_answer_option (option_text, score, order_index, code, question_id)
VALUES ('Self-employed', 8, 3, 'FS-001-OPT3', (SELECT id FROM mifid_question WHERE code = 'FS-001'));
INSERT INTO mifid_answer_option (option_text, score, order_index, code, question_id)
VALUES ('Unemployed', 3, 4, 'FS-001-OPT4', (SELECT id FROM mifid_question WHERE code = 'FS-001'));
-- FS-002
INSERT INTO mifid_answer_option (option_text, score, order_index, code, question_id)
VALUES ('No debts', 10, 1, 'FS-002-OPT1', (SELECT id FROM mifid_question WHERE code = 'FS-002'));
INSERT INTO mifid_answer_option (option_text, score, order_index, code, question_id)
VALUES ('Some debts, manageable', 7, 2, 'FS-002-OPT2', (SELECT id FROM mifid_question WHERE code = 'FS-002'));
INSERT INTO mifid_answer_option (option_text, score, order_index, code, question_id)
VALUES ('Significant debts', 4, 3, 'FS-002-OPT3', (SELECT id FROM mifid_question WHERE code = 'FS-002'));
INSERT INTO mifid_answer_option (option_text, score, order_index, code, question_id)
VALUES ('Prefer not to say', 5, 4, 'FS-002-OPT4', (SELECT id FROM mifid_question WHERE code = 'FS-002'));
-- FS-003
INSERT INTO mifid_answer_option (option_text, score, order_index, code, question_id)
VALUES ('Above 20% of income', 10, 1, 'FS-003-OPT1', (SELECT id FROM mifid_question WHERE code = 'FS-003'));
INSERT INTO mifid_answer_option (option_text, score, order_index, code, question_id)
VALUES ('10-20% of income', 7, 2, 'FS-003-OPT2', (SELECT id FROM mifid_question WHERE code = 'FS-003'));
INSERT INTO mifid_answer_option (option_text, score, order_index, code, question_id)
VALUES ('Less than 10% of income', 4, 3, 'FS-003-OPT3', (SELECT id FROM mifid_question WHERE code = 'FS-003'));
INSERT INTO mifid_answer_option (option_text, score, order_index, code, question_id)
VALUES ('No savings', 2, 4, 'FS-003-OPT4', (SELECT id FROM mifid_question WHERE code = 'FS-003'));

-- questions for Risk Tolerance
INSERT INTO mifid_question (text, question_type, is_required, order_index, code, section_id)
VALUES ('How would you describe your attitude towards investment risk?', 'SINGLE', true, 1, 'RT-001', (SELECT id FROM mifid_section WHERE title = 'Risk Tolerance'));
INSERT INTO mifid_question (text, question_type, is_required, order_index, code, section_id)
VALUES ('What would you do if your investment dropped 20% in value?', 'SINGLE', true, 2, 'RT-002', (SELECT id FROM mifid_section WHERE title = 'Risk Tolerance'));
INSERT INTO mifid_question (text, question_type, is_required, order_index, code, section_id)
VALUES ('What is your main investment goal?', 'SINGLE', true, 3, 'RT-003', (SELECT id FROM mifid_section WHERE title = 'Risk Tolerance'));

-- answer options for Risk Tolerance
-- RT-001
INSERT INTO mifid_answer_option (option_text, score, order_index, code, question_id)
VALUES ('I avoid risk as much as possible', 2, 1, 'RT-001-OPT1', (SELECT id FROM mifid_question WHERE code = 'RT-001'));
INSERT INTO mifid_answer_option (option_text, score, order_index, code, question_id)
VALUES ('I am willing to take some risk for higher returns', 7, 2, 'RT-001-OPT2', (SELECT id FROM mifid_question WHERE code = 'RT-001'));
INSERT INTO mifid_answer_option (option_text, score, order_index, code, question_id)
VALUES ('I seek high risk for potentially high returns', 10, 3, 'RT-001-OPT3', (SELECT id FROM mifid_question WHERE code = 'RT-001'));
INSERT INTO mifid_answer_option (option_text, score, order_index, code, question_id)
VALUES ('I am not sure', 5, 4, 'RT-001-OPT4', (SELECT id FROM mifid_question WHERE code = 'RT-001'));
-- RT-002
INSERT INTO mifid_answer_option (option_text, score, order_index, code, question_id)
VALUES ('Sell all investments to avoid further loss', 2, 1, 'RT-002-OPT1', (SELECT id FROM mifid_question WHERE code = 'RT-002'));
INSERT INTO mifid_answer_option (option_text, score, order_index, code, question_id)
VALUES ('Wait and see if the market recovers', 7, 2, 'RT-002-OPT2', (SELECT id FROM mifid_question WHERE code = 'RT-002'));
INSERT INTO mifid_answer_option (option_text, score, order_index, code, question_id)
VALUES ('Buy more to take advantage of lower prices', 10, 3, 'RT-002-OPT3', (SELECT id FROM mifid_question WHERE code = 'RT-002'));
INSERT INTO mifid_answer_option (option_text, score, order_index, code, question_id)
VALUES ('Consult a financial advisor', 5, 4, 'RT-002-OPT4', (SELECT id FROM mifid_question WHERE code = 'RT-002'));
-- RT-003
INSERT INTO mifid_answer_option (option_text, score, order_index, code, question_id)
VALUES ('Preserve capital', 2, 1, 'RT-003-OPT1', (SELECT id FROM mifid_question WHERE code = 'RT-003'));
INSERT INTO mifid_answer_option (option_text, score, order_index, code, question_id)
VALUES ('Generate income', 7, 2, 'RT-003-OPT2', (SELECT id FROM mifid_question WHERE code = 'RT-003'));
INSERT INTO mifid_answer_option (option_text, score, order_index, code, question_id)
VALUES ('Achieve long-term growth', 10, 3, 'RT-003-OPT3', (SELECT id FROM mifid_question WHERE code = 'RT-003'));
INSERT INTO mifid_answer_option (option_text, score, order_index, code, question_id)
VALUES ('Speculate for high returns', 5, 4, 'RT-003-OPT4', (SELECT id FROM mifid_question WHERE code = 'RT-003'));

-- questions for ESG Preferences
INSERT INTO mifid_question (text, question_type, is_required, order_index, code, section_id)
VALUES ('How important are environmental factors in your investment decisions?', 'SINGLE', true, 1, 'ESG-001', (SELECT id FROM mifid_section WHERE title = 'ESG Preferences'));
INSERT INTO mifid_question (text, question_type, is_required, order_index, code, section_id)
VALUES ('Do you prefer investments with strong social responsibility?', 'SINGLE', true, 2, 'ESG-002', (SELECT id FROM mifid_section WHERE title = 'ESG Preferences'));
INSERT INTO mifid_question (text, question_type, is_required, order_index, code, section_id)
VALUES ('How much do governance practices influence your investment choices?', 'SINGLE', true, 3, 'ESG-003', (SELECT id FROM mifid_section WHERE title = 'ESG Preferences'));

-- answer options for ESG Preferences
-- ESG-001
INSERT INTO mifid_answer_option (option_text, score, order_index, code, question_id)
VALUES ('Very important', 10, 1, 'ESG-001-OPT1', (SELECT id FROM mifid_question WHERE code = 'ESG-001'));
INSERT INTO mifid_answer_option (option_text, score, order_index, code, question_id)
VALUES ('Somewhat important', 7, 2, 'ESG-001-OPT2', (SELECT id FROM mifid_question WHERE code = 'ESG-001'));
INSERT INTO mifid_answer_option (option_text, score, order_index, code, question_id)
VALUES ('Not important', 2, 3, 'ESG-001-OPT3', (SELECT id FROM mifid_question WHERE code = 'ESG-001'));
INSERT INTO mifid_answer_option (option_text, score, order_index, code, question_id)
VALUES ('No opinion', 5, 4, 'ESG-001-OPT4', (SELECT id FROM mifid_question WHERE code = 'ESG-001'));
-- ESG-002
INSERT INTO mifid_answer_option (option_text, score, order_index, code, question_id)
VALUES ('Yes, always', 10, 1, 'ESG-002-OPT1', (SELECT id FROM mifid_question WHERE code = 'ESG-002'));
INSERT INTO mifid_answer_option (option_text, score, order_index, code, question_id)
VALUES ('Sometimes', 7, 2, 'ESG-002-OPT2', (SELECT id FROM mifid_question WHERE code = 'ESG-002'));
INSERT INTO mifid_answer_option (option_text, score, order_index, code, question_id)
VALUES ('Rarely', 2, 3, 'ESG-002-OPT3', (SELECT id FROM mifid_question WHERE code = 'ESG-002'));
INSERT INTO mifid_answer_option (option_text, score, order_index, code, question_id)
VALUES ('No preference', 5, 4, 'ESG-002-OPT4', (SELECT id FROM mifid_question WHERE code = 'ESG-002'));
-- ESG-003
INSERT INTO mifid_answer_option (option_text, score, order_index, code, question_id)
VALUES ('Strongly influence', 10, 1, 'ESG-003-OPT1', (SELECT id FROM mifid_question WHERE code = 'ESG-003'));
INSERT INTO mifid_answer_option (option_text, score, order_index, code, question_id)
VALUES ('Some influence', 7, 2, 'ESG-003-OPT2', (SELECT id FROM mifid_question WHERE code = 'ESG-003'));
INSERT INTO mifid_answer_option (option_text, score, order_index, code, question_id)
VALUES ('Little influence', 2, 3, 'ESG-003-OPT3', (SELECT id FROM mifid_question WHERE code = 'ESG-003'));
INSERT INTO mifid_answer_option (option_text, score, order_index, code, question_id)
VALUES ('No influence', 5, 4, 'ESG-003-OPT4', (SELECT id FROM mifid_question WHERE code = 'ESG-003'));

-- Insert Risk Profile Configurations
INSERT INTO risk_profile_configuration (profile_code, min_score, max_score, profile_label, questionnaire_id)
VALUES ('CONSERVATIVE', 0, 10, 'Conservative Profile', (SELECT id FROM mifid_questionnaire WHERE name = 'Mifid 2025'));
INSERT INTO risk_profile_configuration (profile_code, min_score, max_score, profile_label, questionnaire_id)
VALUES ('AGGRESSIVE', 11, 20, 'Aggressive Profile', (SELECT id FROM mifid_questionnaire WHERE name = 'Mifid 2025'));
