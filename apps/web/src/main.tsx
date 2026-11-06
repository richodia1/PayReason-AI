import React from 'react'
import ReactDOM from 'react-dom/client'
import { createTheme, CssBaseline, ThemeProvider } from '@mui/material'
import App from './App'
import './styles.css'

const theme = createTheme({
  palette: {
    primary: { main: '#13725f' },
    secondary: { main: '#4664c8' },
    background: { default: '#f7f9fa', paper: '#ffffff' },
    text: { primary: '#182b30', secondary: '#65757b' },
  },
  shape: { borderRadius: 6 },
  typography: { fontFamily: 'Inter, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
    button: { textTransform: 'none', fontWeight: 600, letterSpacing: 0 },
    allVariants: { letterSpacing: 0 } },
  components: {
    MuiButton: { defaultProps: { disableElevation: true }, styleOverrides: { root: { minHeight: 40 } } },
    MuiTextField: { defaultProps: { size: 'small', fullWidth: true } },
    MuiChip: { styleOverrides: { root: { fontWeight: 600, borderRadius: 5 } } },
  },
})

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode><ThemeProvider theme={theme}><CssBaseline /><App /></ThemeProvider></React.StrictMode>,
)
