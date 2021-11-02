<!DOCTYPE html>
<html lang="en">
<head>
    <title><?= $company_name ?> | Easy!Appointments</title>
    <meta http-equiv="content-type" content="text/html; charset=UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no">

    <link rel="icon" type="image/x-icon" href="<?= asset_url('assets/img/favicon.ico') ?>">

    <link rel="stylesheet" type="text/css" href="<?= asset_url('assets/ext/bootstrap/css/bootstrap.min.css') ?>">
    <link rel="stylesheet" type="text/css" href="<?= asset_url('assets/ext/jquery-ui/jquery-ui.min.css') ?>">
    <link rel="stylesheet" type="text/css" href="<?= asset_url('assets/ext/jquery-qtip/jquery.qtip.min.css') ?>">
    <link rel="stylesheet" type="text/css" href="<?= asset_url('assets/ext/trumbowyg/ui/trumbowyg.min.css') ?>">
    <link rel="stylesheet" type="text/css" href="<?= asset_url('assets/css/backend.css') ?>">
    <link rel="stylesheet" type="text/css" href="<?= asset_url('assets/css/general.css') ?>">

    <script src="<?= asset_url('assets/ext/jquery/jquery.min.js') ?>"></script>
    <script src="<?= asset_url('assets/ext/bootstrap/js/bootstrap.min.js') ?>"></script>
    <script src="<?= asset_url('assets/ext/jquery-ui/jquery-ui.min.js') ?>"></script>
    <script src="<?= asset_url('assets/ext/jquery-qtip/jquery.qtip.min.js') ?>"></script>
    <script src="<?= asset_url('assets/ext/datejs/date.js') ?>"></script>
    <script src="<?= asset_url('assets/ext/jquery-mousewheel/jquery.mousewheel.js') ?>"></script>
    <script src="<?= asset_url('assets/ext/trumbowyg/trumbowyg.min.js') ?>"></script>

    <script>
    	// Global JavaScript Variables - Used in all backend pages.
    	var availableLanguages = <?= json_encode($this->config->item('available_languages')) ?>;
    	var EALang = <?= json_encode($this->lang->language) ?>;
    </script>
</head>

<body>
<nav id="header" class="navbar">
    <div class="container-fluid">
        <div class="navbar-header">
            <div id="header-logo" class="navbar-brand">
                <img src="<?= base_url('assets/img/logo.png') ?>">
                <span><?= $company_name ?></span>
            </div>
            
            <button type="button" class="navbar-toggle collapsed" data-toggle="collapse" data-target="#header-menu" 
                    aria-expanded="false" aria-controls="navbar">
                <span class="sr-only">Toggle navigation</span>
                <span class="icon-bar"></span>
                <span class="icon-bar"></span>
                <span class="icon-bar"></span>
            </button>
        </div>
	<div id="header-menu" class="collapse navbar-collapse">
		<ul class="nav navbar-nav navbar-right">
		<li>
	          <a href="/reserve/" class="menu-item">Make Reservation</a>
		</li>
		</ul>

	</div>
        
    </div>
</nav>

<div id="notification" style="display: none;"></div>

<div id="loading" style="display: none;">
    <div class="any-element animation is-loading">
        &nbsp;
    </div>
</div>
