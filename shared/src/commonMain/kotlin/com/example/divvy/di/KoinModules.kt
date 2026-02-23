package com.example.divvy.di

import com.example.divvy.backend.ExpensesRepository
import com.example.divvy.backend.GroupRepository
import com.example.divvy.backend.ProfilesRepository
import com.example.divvy.backend.StubExpensesRepository
import com.example.divvy.backend.StubGroupRepository
import com.example.divvy.backend.SupabaseProfilesRepository
import com.example.divvy.ui.analytics.ViewModels.AnalyticsViewModel
import com.example.divvy.ui.assignitems.ViewModels.AssignItemsViewModel
import com.example.divvy.ui.auth.ViewModels.AuthFlowViewModel
import com.example.divvy.ui.expenses.ViewModels.ExpensesViewModel
import com.example.divvy.ui.groupdetail.ViewModels.GroupDetailViewModel
import com.example.divvy.ui.groups.ViewModels.GroupsViewModel
import com.example.divvy.ui.home.ViewModels.HomeViewModel
import com.example.divvy.ui.ledger.ViewModels.LedgerViewModel
import com.example.divvy.ui.profile.ViewModels.ProfileViewModel
import com.example.divvy.ui.splitexpense.ViewModels.SplitExpenseViewModel
import com.example.divvy.ui.splitpercentage.ViewModels.SplitByPercentageViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val sharedModule = module {
    single<GroupRepository> { StubGroupRepository() }
    single<ProfilesRepository> { SupabaseProfilesRepository() }
    single<ExpensesRepository> { StubExpensesRepository() }

    viewModelOf(::HomeViewModel)
    viewModelOf(::GroupsViewModel)
    viewModel { params -> GroupDetailViewModel(params.get(), get()) }
    viewModel { params -> SplitExpenseViewModel(params.get(), params.get(), params.get(), get()) }
    viewModel { params -> AssignItemsViewModel(params.get(), params.get(), params.get(), get()) }
    viewModel { params -> SplitByPercentageViewModel(params.get(), params.get(), params.get(), get()) }
    viewModelOf(::LedgerViewModel)
    viewModelOf(::AnalyticsViewModel)
    viewModelOf(::ProfileViewModel)
    viewModelOf(::ExpensesViewModel)
    viewModelOf(::AuthFlowViewModel)
}
